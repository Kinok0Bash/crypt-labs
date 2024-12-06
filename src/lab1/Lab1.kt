package lab1

import java.io.File
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList

// Специальные маркеры для отступов
const val SPACE_MARKER = "#SP#"
const val NEWLINE_MARKER = "#NL#"
const val TAB_MARKER = "#TAB#"

// Функция для вычисления вероятностей символов
fun calculateProbabilities(data: String): Map<Char, Double> {
    val frequencies = data.groupingBy { it }.eachCount() // Частота каждого символа
    val total = data.length
    return frequencies.mapValues { it.value.toDouble() / total } // Вычисляем вероятность
}

// Функция для построения дерева Хаффмана
fun buildHuffmanTree(probabilities: Map<Char, Double>): TreeNode {
    val minHeap = PriorityQueue<TreeNode>()
    probabilities.forEach { (symbol, probability) ->
        minHeap.add(TreeNode(symbol, probability))
    }

    while (minHeap.size > 1) {
        val left = minHeap.poll()
        val right = minHeap.poll()
        val merged = TreeNode(probability = left.probability + right.probability, left = left, right = right)
        minHeap.add(merged)
    }

    return minHeap.poll()
}

// Функция для генерации кодов Хаффмана
fun generateCodes(root: TreeNode?, prefix: String, codes: MutableMap<Char, String>) {
    if (root == null) return

    if (root.symbol != '\u0000') {
        codes[root.symbol] = prefix
    }

    generateCodes(root.left, prefix + "0", codes)
    generateCodes(root.right, prefix + "1", codes)
}

// Замена пробелов, переводов строк и табуляций на маркеры
fun replaceWhitespace(data: String): String {
    return data.fold("") { acc, symbol ->
        when (symbol) {
            ' ' -> acc + SPACE_MARKER
            '\n' -> acc + NEWLINE_MARKER
            '\t' -> acc + TAB_MARKER
            else -> acc + symbol
        }
    }
}

// Восстановление пробелов, переводов строк и табуляций из маркеров
fun restoreWhitespace(data: String): String {
    return data.replace(SPACE_MARKER, " ")
        .replace(NEWLINE_MARKER, "\n")
        .replace(TAB_MARKER, "\t")
}

// Кодирование текста с использованием алгоритма LZ78
fun lz78Encode(data: String): List<Pair<Int, Char>> {
    val dictionary = mutableMapOf<String, Int>()
    val encoded = mutableListOf<Pair<Int, Char>>()
    var phrase = ""
    var dictSize = 1

    for (symbol in data) {
        val currentPhrase = phrase + symbol
        if (dictionary.containsKey(currentPhrase)) {
            phrase = currentPhrase
        } else {
            encoded.add(dictionary.getOrDefault(phrase, 0) to symbol)
            dictionary[currentPhrase] = dictSize++
            phrase = ""
        }
    }

    if (phrase.isNotEmpty()) {
        encoded.add(dictionary.getOrDefault(phrase, 0) to '\u0000')
    }

    return encoded
}

fun lz78Decode(encoded: List<Pair<Int, Char>>): String {
    val dictionary = mutableMapOf<Int, String>()
    var decoded = ""
    var dictSize = 1

    try {
        for ((index, symbol) in encoded) {
            val phrase = if (index == 0) symbol.toString() else dictionary[index] + symbol
            decoded += phrase
            dictionary[dictSize++] = phrase
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("Ошибка при декодировании данных: ${e.message}", e)
    }

    return decoded
}

// Кодирование текста с использованием алгоритма Хаффмана
fun huffmanEncode(data: String, probabilities: Map<Char, Double>, root: TreeNode, codes: MutableMap<Char, String>): String {
    generateCodes(root, "", codes)
    return data.fold("") { acc, symbol -> acc + codes[symbol] }
}

// Декодирование данных, закодированных с помощью алгоритма Хаффмана
fun huffmanDecode(encodedText: String, root: TreeNode): String {
    var decoded = ""
    var currentNode = root

    for (bit in encodedText) {
        currentNode = if (bit == '0') currentNode.left!! else currentNode.right!!
        if (currentNode.symbol != '\u0000') {
            decoded += currentNode.symbol
            currentNode = root
        }
    }

    return decoded
}

// Кодирование данных из файла
fun encoding(inputFile: String) {
    val path = "resources/lab1/$inputFile.txt"
    val data = File(path).readText()

    // Замена отступов на маркеры
    val modifiedData = replaceWhitespace(data)

    // 1. Кодирование с Хаффманом
    val probabilities = calculateProbabilities(modifiedData)
    val huffmanRoot = buildHuffmanTree(probabilities)
    val huffmanCodes = mutableMapOf<Char, String>()
    val huffmanEncoded = huffmanEncode(modifiedData, probabilities, huffmanRoot, huffmanCodes)

    val parentDir = File(path).parent
    val huffFile = "$parentDir/$inputFile-huffman.txt"
    File(huffFile).writeText(huffmanEncoded + "\n" + probabilities.entries.joinToString("\n") { "${it.key}: ${it.value}" })

    // 2. Кодирование с LZ78
    val lz78Encoded = lz78Encode(modifiedData)

    val lzFile = "$parentDir/$inputFile-lz78.txt"
    File(lzFile).writeText(lz78Encoded.joinToString("|") { "${it.first},${it.second}" })

    println("Кодирование успешно завершено.")
}

fun decoding(codeFile: String) {
    val lzPath = "resources/lab1/$codeFile-lz78.txt"
    val huffPath = "resources/lab1/$codeFile-huffman.txt"

    val lzFile = File(lzPath)
    val huffFile = File(huffPath)

    if (!lzFile.exists() || !huffFile.exists()) {
        println("Один из файлов для декодирования отсутствует.")
        return
    }

    val lz78Encoded = try {
        lzFile.readText()
            .split("|")
            .filter { it.isNotBlank() }
            .map {
                if (it.contains(",,") && it.indexOf(",,") == it.lastIndex - 1) {
                    // Если строка содержит двойную запятую, разделяем корректно
                    val index = it.substringBefore(",,").toInt()
                    val symbol = ','
                    index to symbol
                } else {
                    val parts = it.split(",")
                    if (parts.size != 2) throw IllegalArgumentException("Некорректный формат данных: $it")
                    parts[0].toInt() to parts[1][0]
                }
            }
    } catch (e: Exception) {
        println("Ошибка при чтении LZ78 данных: ${e.message}")
        return
    }

    val lz78Decoded = try {
        lz78Decode(lz78Encoded)
    } catch (e: IllegalArgumentException) {
        println("Ошибка декодирования LZ78: ${e.message}")
        return
    }

    val huffData = try {
        huffFile.readLines()
    } catch (e: Exception) {
        println("Ошибка при чтении файла Хаффмана: ${e.message}")
        return
    }

    if (huffData.isEmpty()) {
        println("Файл Хаффмана пуст.")
        return
    }

    val huffmanEncoded = huffData[0]
    val probabilities = mutableMapOf<Char, Double>()

    val huffArray = ArrayList<String>()

    for (i in huffData.indices) {
        if (i == 0) continue
        huffArray.add(huffData[i])
    }

    try {
        huffArray.filter { it.isNotBlank() }.forEach { line ->
            val parts = line.split(": ")
            if (parts.size != 2) {
                throw IllegalArgumentException("Некорректный формат строки вероятностей: $line")
            }
            var symbol: Char
            try {
                symbol = if (parts[0] == " ") ' ' else parts[0].first() // Обрабатываем пробелы
            } catch (e: NoSuchElementException) {
                symbol = '\n'
            }
            val probability = parts[1].toDouble()
            probabilities[symbol] = probability
        }
    } catch (e: Exception) {
        println("Ошибка при чтении вероятностей символов Хаффмана: ${e.javaClass.simpleName} - ${e.message}")
        return
    }

    val huffmanRoot = buildHuffmanTree(probabilities)
    val huffmanDecoded = try {
        huffmanDecode(huffmanEncoded, huffmanRoot)
    } catch (e: Exception) {
        println("Ошибка декодирования Хаффмана: ${e.message}")
        return
    }

    val restoredData = restoreWhitespace(lz78Decoded)

    val parentPath = File(lzPath).parent
    val outputFile = "$parentPath/$codeFile-decoded.txt"
    try {
        File(outputFile).writeText(restoredData)
        println("Декодирование успешно завершено. Результат сохранён в файл '$outputFile'.")
    } catch (e: Exception) {
        println("Ошибка при записи декодированных данных: ${e.message}")
    }
}

// Главная функция
fun main() {
    while (true) {
        println("Выберите опцию:\n1. Кодировать файл\n2. Декодировать файл\n3. Выйти")
        when (readLine()!!.toInt()) {
            1 -> {
                println("Введите имя файла для кодирования: ")
                val inputFile = readLine()!!
                encoding(inputFile)
            }
            2 -> {
                println("Введите имя файла для декодирования: ")
                val codeFile = readLine()!!
                decoding(codeFile)
            }
            3 -> return
            else -> println("Неверный выбор")
        }
    }
}
