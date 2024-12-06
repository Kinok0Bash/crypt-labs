package lab3

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun extractPixels(imagePath: String): List<Triple<Int, Int, Int>> {
    val image: BufferedImage = ImageIO.read(File(imagePath))
    val pixels = mutableListOf<Triple<Int, Int, Int>>()

    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val rgb = image.getRGB(x, y)
            val red = (rgb shr 16) and 0xFF
            val green = (rgb shr 8) and 0xFF
            val blue = rgb and 0xFF
            pixels.add(Triple(red, green, blue))
        }
    }
    return pixels
}

fun calculateColorProbabilities(pixels: List<Triple<Int, Int, Int>>): Map<Int, Double> {
    val frequencies = mutableMapOf<Int, Int>()
    pixels.forEach { (r, g, b) ->
        listOf(r, g, b).forEach { value -> frequencies[value] = frequencies.getOrDefault(value, 0) + 1 }
    }
    val total = pixels.size * 3
    return frequencies.mapValues { it.value.toDouble() / total }
}

fun lz78EncodePixels(pixels: List<Triple<Int, Int, Int>>): List<Pair<Int, Triple<Int, Int, Int>?>> {
    val dictionary = mutableMapOf<List<Triple<Int, Int, Int>>, Int>()
    val encoded = mutableListOf<Pair<Int, Triple<Int, Int, Int>?>>()
    var phrase = mutableListOf<Triple<Int, Int, Int>>()
    var dictSize = 1

    for (pixel in pixels) {
        phrase.add(pixel)
        if (!dictionary.containsKey(phrase)) {
            encoded.add(dictionary.getOrDefault(phrase.dropLast(1), 0) to pixel)
            dictionary[phrase.toList()] = dictSize++
            phrase.clear()
        }
    }

    if (phrase.isNotEmpty()) {
        encoded.add(dictionary.getOrDefault(phrase, 0) to null)
    }

    return encoded
}

fun saveImage(pixels: List<Triple<Int, Int, Int>>, width: Int, height: Int, outputPath: String) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val (r, g, b) = pixels[index++]
            val rgb = (r shl 16) or (g shl 8) or b
            image.setRGB(x, y, rgb)
        }
    }
    ImageIO.write(image, "bmp", File(outputPath))
}

fun codeDecode(fileName: String) {
    val filePath = "$inDir$fileName.bmp"
    val pixels = extractPixels(filePath)
    val probabilities = calculateColorProbabilities(pixels)
    val huffmanTree = buildHuffmanTree(probabilities)
    val huffmanCodes = mutableMapOf<Int, String>()
    val huffmanEncoded = huffmanEncode(pixels.flatMap { listOf(it.first, it.second, it.third) }, probabilities, huffmanTree, huffmanCodes)
    val decodedPixels = huffmanDecode(huffmanEncoded, huffmanTree).chunked(3).map { Triple(it[0], it[1], it[2]) }
    saveImage(decodedPixels, width, height, "output.png")
}

fun main() {
    while (true) {
        print("Введите название BMP файла (без расширения): ")
        val fileName = readLine() ?: continue
        println()
        println("Выберите действие:")
        println("1. Архиватор")
        println("0. Выход")
        println()
        print("Ответ: ")
        when (readLine()) {
            "1" -> codeDecode(fileName)
            "0" -> return
            else -> println("Неверный выбор, попробуйте снова.")
        }
        println()
    }
}