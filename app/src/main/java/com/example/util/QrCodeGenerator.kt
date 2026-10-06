package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Clean, lightweight self-contained QR Code Generator for Android.
 * Supports Byte mode with Reed-Solomon Error Correction (Level M).
 * Generates standard 2D Boolean matrices and Bitmaps readable by any QR scanner.
 */
object QrCodeGenerator {

    // Predefined QR version specs: Version 1 (21x21), Version 2 (25x25), Version 3 (29x29), Version 4 (33x33)
    data class QrSpec(val version: Int, val size: Int, val totalDataCodewords: Int, val ecCodewords: Int)

    private val specs = listOf(
        QrSpec(1, 21, 16, 10),
        QrSpec(2, 25, 28, 16),
        QrSpec(3, 29, 44, 26),
        QrSpec(4, 33, 64, 36),
        QrSpec(5, 37, 86, 48),
        QrSpec(6, 41, 108, 64)
    )

    fun encode(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        val spec = specs.firstOrNull { it.totalDataCodewords >= bytes.size + 3 } ?: specs.last()
        val size = spec.size

        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // 1. Finder patterns (Top-Left, Top-Right, Bottom-Left)
        drawFinderPattern(matrix, reserved, 0, 0)
        drawFinderPattern(matrix, reserved, size - 7, 0)
        drawFinderPattern(matrix, reserved, 0, size - 7)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            val v = (i % 2 == 0)
            matrix[6][i] = v
            reserved[6][i] = true
            matrix[i][6] = v
            reserved[i][6] = true
        }

        // 3. Dark module and format timing reservations
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        for (i in 0..8) {
            reserved[i][8] = true
            reserved[8][i] = true
            reserved[size - 1 - i][8] = true
            reserved[8][size - 1 - i] = true
        }

        // 4. Alignment pattern for v >= 2
        if (spec.version >= 2) {
            val alignPos = when (spec.version) {
                2 -> 18
                3 -> 22
                4 -> 26
                5 -> 30
                else -> 34
            }
            drawAlignmentPattern(matrix, reserved, alignPos, alignPos)
        }

        // 5. Build bit stream (Byte mode: 0100 + char count + data)
        val bitBuffer = mutableListOf<Boolean>()
        // Byte mode indicator: 0100
        appendBits(bitBuffer, 4, 4)
        // Character count indicator (8 bits for v1-9 byte mode)
        appendBits(bitBuffer, bytes.size, 8)
        // Data bytes
        for (b in bytes) {
            appendBits(bitBuffer, b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zeroes)
        val maxDataBits = spec.totalDataCodewords * 8
        var termLen = 4
        if (bitBuffer.size + termLen > maxDataBits) termLen = maxDataBits - bitBuffer.size
        for (i in 0 until termLen) bitBuffer.add(false)

        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0 && bitBuffer.size < maxDataBits) {
            bitBuffer.add(false)
        }

        // Pad bytes 0xEC and 0x11
        var padToggle = true
        while (bitBuffer.size < maxDataBits) {
            appendBits(bitBuffer, if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        // Split into data codewords
        val dataCodewords = IntArray(spec.totalDataCodewords)
        for (i in 0 until spec.totalDataCodewords) {
            var byteVal = 0
            for (b in 0..7) {
                if (bitBuffer[i * 8 + b]) {
                    byteVal = byteVal or (1 shl (7 - b))
                }
            }
            dataCodewords[i] = byteVal
        }

        // Compute Reed-Solomon Error Correction
        val ecCodewords = computeReedSolomon(dataCodewords, spec.ecCodewords)

        // Combine all codewords
        val finalBits = mutableListOf<Boolean>()
        for (cw in dataCodewords) appendBits(finalBits, cw, 8)
        for (cw in ecCodewords) appendBits(finalBits, cw, 8)

        // 6. Place data into matrix with Mask Pattern 0: (row + col) % 2 == 0
        var bitIdx = 0
        var upward = true
        var col = size - 1

        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing line

            val rowRange = if (upward) (size - 1 downTo 0) else (0 until size)
            for (row in rowRange) {
                for (cOffset in 0..1) {
                    val currCol = col - cOffset
                    if (!reserved[row][currCol]) {
                        var bit = if (bitIdx < finalBits.size) finalBits[bitIdx++] else false
                        // Mask 0: flip if (row + col) % 2 == 0
                        if ((row + currCol) % 2 == 0) {
                            bit = !bit
                        }
                        matrix[row][currCol] = bit
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 7. Write Format Information (Error Correction Level M + Mask 0)
        // Pre-computed BCH format string for Level M, Mask 0 with XOR mask 0x5412: 101010000010010
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        // Top-left
        val formatCoords1 = listOf(
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5), Pair(8, 7), Pair(8, 8),
            Pair(7, 8), Pair(5, 8), Pair(4, 8), Pair(3, 8), Pair(2, 8), Pair(1, 8), Pair(0, 8)
        )
        for (i in 0..14) {
            val (r, c) = formatCoords1[i]
            matrix[r][c] = (formatBits[i] == 1)
        }

        // Bottom-left / Top-right split
        for (i in 0..6) {
            matrix[size - 1 - i][8] = (formatBits[i] == 1)
        }
        for (i in 7..14) {
            matrix[8][size - 15 + i] = (formatBits[i] == 1)
        }

        return matrix
    }

    private fun drawFinderPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int) {
        for (dr in -1..7) {
            for (dc in -1..7) {
                val row = r + dr
                val col = c + dc
                if (row in matrix.indices && col in matrix.indices) {
                    reserved[row][col] = true
                    val inCenter = dr in 0..6 && dc in 0..6
                    if (inCenter) {
                        val isBlack = (dr == 0 || dr == 6 || dc == 0 || dc == 6) || (dr in 2..4 && dc in 2..4)
                        matrix[row][col] = isBlack
                    } else {
                        matrix[row][col] = false
                    }
                }
            }
        }
    }

    private fun drawAlignmentPattern(matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, r: Int, c: Int) {
        for (dr in -2..2) {
            for (dc in -2..2) {
                val row = r + dr
                val col = c + dc
                if (row in matrix.indices && col in matrix.indices && !reserved[row][col]) {
                    reserved[row][col] = true
                    matrix[row][col] = (dr == -2 || dr == 2 || dc == -2 || dc == 2 || (dr == 0 && dc == 0))
                }
            }
        }
    }

    private fun appendBits(buffer: MutableList<Boolean>, value: Int, count: Int) {
        for (i in count - 1 downTo 0) {
            buffer.add(((value ushr i) and 1) == 1)
        }
    }

    // GF(256) Reed-Solomon polynomial division
    private val expTable = IntArray(256)
    private val logTable = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            expTable[i] = x
            logTable[x] = i
            x = (x shl 1) xor if (x >= 128) 0x11D else 0
        }
        expTable[255] = expTable[0]
    }

    private fun gfMultiply(x: Int, y: Int): Int {
        if (x == 0 || y == 0) return 0
        return expTable[(logTable[x] + logTable[y]) % 255]
    }

    private fun computeReedSolomon(data: IntArray, ecCount: Int): IntArray {
        // Compute generator polynomial of degree ecCount
        var gen = intArrayOf(1)
        for (i in 0 until ecCount) {
            val root = expTable[i]
            val nextGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMultiply(gen[j], root)
                nextGen[j + 1] = nextGen[j + 1] xor gen[j]
            }
            gen = nextGen
        }

        // Polynomial remainder
        val res = IntArray(ecCount)
        for (b in data) {
            val factor = b xor res[0]
            System.arraycopy(res, 1, res, 0, ecCount - 1)
            res[ecCount - 1] = 0
            for (j in 0 until ecCount) {
                res[j] = res[j] xor gfMultiply(gen[j], factor)
            }
        }
        return res
    }

    fun generateBitmap(text: String, sizePx: Int = 300, quietZone: Int = 4): Bitmap {
        val matrix = encode(text)
        val matrixSize = matrix.size
        val totalSize = matrixSize + (quietZone * 2)

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sizePx * sizePx)

        for (y in 0 until sizePx) {
            val my = (y * totalSize) / sizePx - quietZone
            for (x in 0 until sizePx) {
                val mx = (x * totalSize) / sizePx - quietZone
                val isBlack = if (my in 0 until matrixSize && mx in 0 until matrixSize) {
                    matrix[my][mx]
                } else {
                    false
                }
                pixels[y * sizePx + x] = if (isBlack) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bitmap
    }

    fun generateImageBitmap(text: String, sizePx: Int = 300): ImageBitmap {
        return generateBitmap(text, sizePx).asImageBitmap()
    }
}
