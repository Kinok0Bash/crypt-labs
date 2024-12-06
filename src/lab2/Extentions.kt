package lab2


fun ByteArray.toLittleEndianInt(): Int {
    return this[0].toInt() and 0xFF or
            (this[1].toInt() and 0xFF shl 8) or
            (this[2].toInt() and 0xFF shl 16) or
            (this[3].toInt() and 0xFF shl 24)
}

fun ByteArray.toLittleEndianShort(): Short {
    return ((this[0].toInt() and 0xFF) or
            ((this[1].toInt() and 0xFF) shl 8)).toShort()
}

fun Int.toLittleEndianBytes(): ByteArray {
    return byteArrayOf(
        (this and 0xFF).toByte(),
        (this shr 8 and 0xFF).toByte(),
        (this shr 16 and 0xFF).toByte(),
        (this shr 24 and 0xFF).toByte()
    )
}

fun Short.toLittleEndianBytes(): ByteArray {
    return byteArrayOf(
        (this.toInt() and 0xFF).toByte(),
        (this.toInt() shr 8 and 0xFF).toByte()
    )
}
