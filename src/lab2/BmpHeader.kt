package lab2

data class BmpHeader(
    val bfType: String,
    val bfSize: Int,
    val bfOffBits: Int,
    val biWidth: Int,
    val biHeight: Int,
    val biBitCount: Short,
    val biPlanes: Short,
    val biCompression: Int,
    val biSizeImage: Int,
    val biXPelsPerMeter: Int,
    val biYPelsPerMeter: Int,
    val biClrUsed: Int,
    val biClrImportant: Int
)
