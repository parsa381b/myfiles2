package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Lightweight, self-contained QR Code generator for URLs and text up to 100 characters.
 * Generates standard QR Code Version 2/3/4 with byte mode and Reed-Solomon ECC.
 */
object QrCodeGenerator {

    fun generateMatrix(text: String): Array<BooleanArray> {
        val bytes = text.toByteArray(Charsets.ISO_8859_1)
        val version = when {
            bytes.size <= 17 -> 1
            bytes.size <= 32 -> 2
            bytes.size <= 53 -> 3
            else -> 4
        }
        val size = 17 + 4 * version
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        fun setModule(r: Int, c: Int, isDark: Boolean, isReserved: Boolean = true) {
            if (r in 0 until size && c in 0 until size) {
                matrix[r][c] = isDark
                if (isReserved) reserved[r][c] = true
            }
        }

        fun drawFinderPattern(row: Int, col: Int) {
            for (r in -1..7) {
                for (c in -1..7) {
                    val isBorder = r in 0..6 && (c == 0 || c == 6) || c in 0..6 && (r == 0 || r == 6)
                    val isCenter = r in 2..4 && c in 2..4
                    val isWhite = (r in 1..5 && (c == 1 || c == 5)) || (c in 1..5 && (r == 1 || r == 5))
                    val inside = r in 0..6 && c in 0..6
                    val dark = (inside && (isBorder || isCenter)) && !isWhite
                    val tr = row + r
                    val tc = col + c
                    if (tr in 0 until size && tc in 0 until size) {
                        setModule(tr, tc, dark, true)
                    }
                }
            }
        }

        // 1. Finder patterns at top-left, top-right, bottom-left
        drawFinderPattern(0, 0)
        drawFinderPattern(0, size - 7)
        drawFinderPattern(size - 7, 0)

        // 2. Timing patterns
        for (i in 8 until size - 8) {
            val dark = i % 2 == 0
            setModule(6, i, dark, true)
            setModule(i, 6, dark, true)
        }

        // 3. Alignment pattern for version >= 2
        if (version >= 2) {
            val alignPos = when (version) {
                2 -> 18
                3 -> 22
                4 -> 26
                else -> 18
            }
            for (r in -2..2) {
                for (c in -2..2) {
                    val dark = r == -2 || r == 2 || c == -2 || c == 2 || (r == 0 && c == 0)
                    setModule(alignPos + r, alignPos + c, dark, true)
                }
            }
        }

        // Dark module
        setModule(4 * version + 9, 8, true, true)

        // Format info reservation
        for (i in 0..8) {
            setModule(8, i, false, true)
            setModule(i, 8, false, true)
            setModule(8, size - 1 - i, false, true)
            setModule(size - 1 - i, 8, false, true)
        }

        // Encode data into bits: Byte mode (0100) + Count (8 bits) + Data bytes + Terminator (0000)
        val bitBuffer = mutableListOf<Int>()
        fun addBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bitBuffer.add((value ushr i) and 1)
            }
        }

        addBits(0b0100, 4) // 8-bit byte mode
        addBits(bytes.size, 8)
        for (b in bytes) {
            addBits(b.toInt() and 0xFF, 8)
        }
        // Terminator
        val capacityBytes = when (version) {
            1 -> 19
            2 -> 34
            3 -> 55
            else -> 80
        }
        val capacityBits = capacityBytes * 8
        val padCount = (capacityBits - bitBuffer.size).coerceAtLeast(0)
        val termZeros = padCount.coerceAtMost(4)
        repeat(termZeros) { bitBuffer.add(0) }
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(0)
        }
        val padBytes = listOf(0xEC, 0x11)
        var padIndex = 0
        while (bitBuffer.size < capacityBits) {
            addBits(padBytes[padIndex % 2], 8)
            padIndex++
        }

        // Reed-Solomon Error Correction
        val dataCodewords = IntArray(capacityBytes)
        for (i in 0 until capacityBytes) {
            var b = 0
            for (j in 0 until 8) {
                b = (b shl 1) or bitBuffer[i * 8 + j]
            }
            dataCodewords[i] = b
        }

        val eccCount = when (version) {
            1 -> 7
            2 -> 10
            3 -> 15
            else -> 20
        }

        val eccCodewords = generateEcc(dataCodewords, eccCount)
        val allCodewords = dataCodewords + eccCodewords

        // Convert codewords to final bitstream
        val finalBits = mutableListOf<Int>()
        for (cw in allCodewords) {
            for (i in 7 downTo 0) {
                finalBits.add((cw ushr i) and 1)
            }
        }

        // Place data bits in matrix (right-to-left 2-column zig-zag)
        var bitIndex = 0
        var up = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // skip vertical timing line
            val rows = if (up) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (c in 0..1) {
                    val curCol = col - c
                    if (!reserved[row][curCol]) {
                        val bit = if (bitIndex < finalBits.size) finalBits[bitIndex++] else 0
                        // Apply Mask 0: (row + col) % 2 == 0
                        val mask = (row + curCol) % 2 == 0
                        matrix[row][curCol] = (bit == 1) xor mask
                    }
                }
            }
            up = !up
            col -= 2
        }

        // Apply format information (Mask 0, ECC level L: 0b01)
        val formatBits = 0b111011111000100 // Precomputed Mask 0, ECC L with BCH
        for (i in 0..14) {
            val bit = ((formatBits ushr (14 - i)) and 1) == 1
            if (i <= 5) {
                matrix[8][i] = bit
            } else if (i == 6 || i == 7) {
                matrix[8][i + 1] = bit
            } else {
                matrix[14 - i][8] = bit
            }

            if (i <= 7) {
                matrix[size - 1 - i][8] = bit
            } else {
                matrix[8][size - 15 + i] = bit
            }
        }

        return matrix
    }

    // GF(256) Reed-Solomon computation
    private fun generateEcc(data: IntArray, eccCount: Int): IntArray {
        val gfExp = IntArray(512)
        val gfLog = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfExp[i + 255] = x
            gfLog[x] = i
            x = x shl 1
            if (x >= 256) x = x xor 0x11D
        }

        fun gfMul(a: Int, b: Int): Int {
            if (a == 0 || b == 0) return 0
            return gfExp[gfLog[a] + gfLog[b]]
        }

        // Generator polynomial
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val nextGen = IntArray(gen.size + 1)
            val factor = gfExp[i]
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMul(gen[j], factor)
                nextGen[j + 1] = nextGen[j + 1] xor gen[j]
            }
            gen = nextGen
        }

        // Division
        val remainder = IntArray(eccCount)
        for (byte in data) {
            val factor = byte xor remainder[0]
            for (j in 0 until eccCount - 1) {
                remainder[j] = remainder[j + 1] xor gfMul(gen[eccCount - 1 - j], factor)
            }
            remainder[eccCount - 1] = gfMul(gen[0], factor)
        }

        return remainder
    }
}

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White
) {
    val matrix = remember(data) {
        try {
            QrCodeGenerator.generateMatrix(data)
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(lightColor)
            .padding(12.dp)
            .aspectRatio(1f)
    ) {
        if (matrix != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val moduleSize = size.width / matrix.size
                for (r in matrix.indices) {
                    for (c in matrix[r].indices) {
                        if (matrix[r][c]) {
                            drawRect(
                                color = darkColor,
                                topLeft = Offset(c * moduleSize, r * moduleSize),
                                size = Size(moduleSize + 0.5f, moduleSize + 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
