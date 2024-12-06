package lab2

import java.io.File
import kotlin.math.roundToInt

val inDir = "resources/lab2/"
val outDir = "resources/lab2/out/"

fun checkBmp(fileName: String, displayInfo: Boolean = true): BmpHeader? {
    val filePath = "$inDir$fileName.bmp"
    val file = File(filePath)
    if (!file.exists()) {
        println("Файл '$filePath' не найден.")
        return null
    }

    file.inputStream().use { input ->
        val fileHeader = ByteArray(14)
        input.read(fileHeader)

        val bfType = String(fileHeader.copyOfRange(0, 2))
        if (bfType != "BM") {
            throw IllegalArgumentException("Файл не является BMP.")
        }

        val bfSize = fileHeader.copyOfRange(2, 6).toLittleEndianInt()
        val bfOffBits = fileHeader.copyOfRange(10, 14).toLittleEndianInt()

        val infoHeader = ByteArray(40)
        input.read(infoHeader)

        val biWidth = infoHeader.copyOfRange(4, 8).toLittleEndianInt()
        val biHeight = infoHeader.copyOfRange(8, 12).toLittleEndianInt()
        val biPlanes = infoHeader.copyOfRange(12, 14).toLittleEndianShort()
        val biBitCount = infoHeader.copyOfRange(14, 16).toLittleEndianShort()
        val biCompression = infoHeader.copyOfRange(16, 20).toLittleEndianInt()
        val biSizeImage = infoHeader.copyOfRange(20, 24).toLittleEndianInt()
        val biXPelsPerMeter = infoHeader.copyOfRange(24, 28).toLittleEndianInt()
        val biYPelsPerMeter = infoHeader.copyOfRange(28, 32).toLittleEndianInt()
        val biClrUsed = infoHeader.copyOfRange(32, 36).toLittleEndianInt()
        val biClrImportant = infoHeader.copyOfRange(36, 40).toLittleEndianInt()

        if (displayInfo) {
            println("Информация о BMP файле:")
            println("Тип файла: $bfType")
            println("Размер файла: $bfSize байт")
            println("Смещение пиксельных данных: $bfOffBits байт")
            println("Размер заголовка BITMAP: ${infoHeader.copyOfRange(0, 4).toLittleEndianInt()}")
            println("Ширина изображения: $biWidth пикселей")
            println("Высота изображения: $biHeight пикселей")
            println("Количество плоскостей: $biPlanes")
            println("Бит на пиксель: $biBitCount")
            println("Тип сжатия: $biCompression")
            println("Размер изображения: $biSizeImage байт")
            println("Горизонтальное разрешение: $biXPelsPerMeter пикселей/метр")
            println("Вертикальное разрешение: $biYPelsPerMeter пикселей/метр")
            println("Количество используемых цветов: $biClrUsed")
            println("Количество 'важных' цветов: $biClrImportant")
        }

        return BmpHeader(bfType, bfSize, bfOffBits, biWidth, biHeight, biBitCount, biPlanes, biCompression, biSizeImage, biXPelsPerMeter, biYPelsPerMeter, biClrUsed, biClrImportant)
    }
}

fun rgbChannel(fileName: String) {
    val filePath = "$inDir$fileName.bmp"
    val header = checkBmp(filePath, displayInfo = false) ?: return
    val file = File(filePath)
    val pixelData = file.readBytes().drop(header.bfOffBits).toByteArray()

    val rowPadded = ((header.biWidth * 3 + 3) and 0xFFFFFFFC.toInt())
    val blueChannel = pixelData.copyOf()
    val greenChannel = pixelData.copyOf()
    val redChannel = pixelData.copyOf()

    for (y in 0..<header.biHeight) {
        for (x in 0..<header.biWidth) {
            val idx = y * rowPadded + x * 3
            blueChannel[idx + 1] = 0
            blueChannel[idx + 2] = 0
            greenChannel[idx] = 0
            greenChannel[idx + 2] = 0
            redChannel[idx] = 0
            redChannel[idx + 1] = 0
        }
    }

    val outputDir = File("$outDir$fileName")
    outputDir.mkdirs()

    saveBmp("${outputDir.path}/blue.bmp", header, blueChannel)
    saveBmp("${outputDir.path}/green.bmp", header, greenChannel)
    saveBmp("${outputDir.path}/red.bmp", header, redChannel)

    println("Каналы RGB успешно сохранены в '${outputDir.path}'.")
}

fun bmpSlices(fileName: String) {
    val filePath = "$inDir$fileName.bmp"
    val header = checkBmp(filePath, displayInfo = false) ?: return
    val file = File(filePath)
    val pixelData = file.readBytes().drop(header.bfOffBits).toByteArray()

    val rowPadded = (header.biWidth * 3 + 3) and 0xFFFFFFFC.toInt()
    val image = IntArray(header.biWidth * header.biHeight)

    for (y in 0..<header.biHeight) {
        for (x in 0..<header.biWidth) {
            val idx = y * rowPadded + x * 3
            val blue = pixelData[idx].toInt() and 0xFF
            val green = pixelData[idx + 1].toInt() and 0xFF
            val red = pixelData[idx + 2].toInt() and 0xFF
            image[y * header.biWidth + x] = (0.2989 * red + 0.5870 * green + 0.1140 * blue).roundToInt()
        }
    }

    val bitSlices = (0..7).map { bit ->
        ByteArray(image.size) { idx ->
            if ((image[idx] shr bit) and 0x01 == 1) 255.toByte() else 0.toByte()
        }
    }

    val outputDir = File("$outDir$fileName")
    outputDir.mkdirs()

    bitSlices.forEachIndexed { i, slice ->
        val slicePixelData = ByteArray(rowPadded * header.biHeight)
        for (y in 0..<header.biHeight) {
            for (x in 0..<header.biWidth) {
                val value = slice[y * header.biWidth + x]
                val idx = y * rowPadded + x * 3
                slicePixelData[idx] = value
                slicePixelData[idx + 1] = value
                slicePixelData[idx + 2] = value
            }
        }
        saveBmp("$outputDir/slice$i.bmp", header, slicePixelData)
    }

    println("Битовые срезы сохранены в '$outputDir'")
}

fun saveBmp(filename: String, header: BmpHeader, pixelData: ByteArray) {
    val file = File(filename)
    file.outputStream().use { output ->
        output.write(header.bfType.toByteArray())
        output.write(header.bfSize.toLittleEndianBytes())
        output.write(ByteArray(4)) // Reserved
        output.write(header.bfOffBits.toLittleEndianBytes())
        output.write(40.toLittleEndianBytes()) // Header size
        output.write(header.biWidth.toLittleEndianBytes())
        output.write(header.biHeight.toLittleEndianBytes())
        output.write(1.toShort().toLittleEndianBytes()) // Planes
        output.write(header.biBitCount.toLittleEndianBytes())
        output.write(ByteArray(24)) // Padding
        output.write(pixelData)
    }
}

fun main() {
    while (true) {
        print("Введите название BMP файла (без расширения): ")
        val fileName = readLine() ?: continue
        println()
        println("Выберите действие:")
        println("1. Показать заголовок BMP")
        println("2. Разделить каналы RGB")
        println("3. Получить битовые срезы")
        println("0. Выход")
        println()
        print("Ответ: ")
        when (readLine()) {
            "1" -> checkBmp(fileName)
            "2" -> rgbChannel(fileName)
            "3" -> bmpSlices(fileName)
            "0" -> return
            else -> println("Неверный выбор, попробуйте снова.")
        }
        println()
    }
}
