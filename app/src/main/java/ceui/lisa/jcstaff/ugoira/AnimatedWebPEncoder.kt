package ceui.lisa.jcstaff.ugoira

import android.graphics.Bitmap
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds an animated WebP RIFF container from a list of Bitmap frames.
 *
 * Each frame is individually compressed via Bitmap.compress() to get a VP8/VP8L bitstream,
 * then wrapped in ANMF chunks inside a VP8X + ANIM animated WebP file.
 */
object AnimatedWebPEncoder {

    fun encode(
        frames: List<Pair<Bitmap, Int>>,  // Bitmap + delay in milliseconds
        outputStream: OutputStream
    ) {
        if (frames.isEmpty()) return

        val width = frames[0].first.width
        val height = frames[0].first.height

        // Encode each frame to a single-frame WebP, then extract the inner bitstream
        val frameDataList = frames.map { (bitmap, delayMs) ->
            val bos = ByteArrayOutputStream()
            @Suppress("DEPRECATION")
            val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(compressFormat, 85, bos)
            val bitstreamData = extractBitstreamData(bos.toByteArray())
            Pair(bitstreamData, delayMs)
        }

        val vp8xChunk = buildVP8XChunk(width, height)
        val animChunk = buildANIMChunk()
        val anmfChunks = frameDataList.map { (data, delayMs) ->
            buildANMFChunk(width, height, delayMs, data)
        }

        // RIFF payload = "WEBP" + all chunks
        val totalChunksSize = vp8xChunk.size + animChunk.size + anmfChunks.sumOf { it.size }
        val riffPayloadSize = 4 + totalChunksSize

        val header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(riffPayloadSize)
        header.put("WEBP".toByteArray())
        outputStream.write(header.array())

        outputStream.write(vp8xChunk)
        outputStream.write(animChunk)
        anmfChunks.forEach { outputStream.write(it) }
    }

    /**
     * Extract everything after the 12-byte "RIFF xxxx WEBP" header.
     * This is the raw VP8/VP8L/VP8X bitstream block suitable for ANMF frame data.
     */
    private fun extractBitstreamData(webpBytes: ByteArray): ByteArray {
        // Validate RIFF/WEBP header
        if (webpBytes.size < 12) return webpBytes
        return webpBytes.copyOfRange(12, webpBytes.size)
    }

    private fun buildVP8XChunk(width: Int, height: Int): ByteArray {
        // VP8X chunk data = 10 bytes: flags (4) + canvas_w-1 (3) + canvas_h-1 (3)
        val buf = ByteBuffer.allocate(18).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("VP8X".toByteArray())
        buf.putInt(10)          // chunk data size
        buf.putInt(0x00000002)  // flags: bit 1 = animation
        putUint24(buf, width - 1)
        putUint24(buf, height - 1)
        return buf.array()
    }

    private fun buildANIMChunk(): ByteArray {
        // ANIM chunk data = 6 bytes: background_color (4, BGRA) + loop_count (2)
        val buf = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("ANIM".toByteArray())
        buf.putInt(6)
        buf.putInt(0xFFFFFFFF.toInt())  // white background (BGRA)
        buf.putShort(0)                  // loop count: 0 = infinite
        return buf.array()
    }

    private fun buildANMFChunk(width: Int, height: Int, delayMs: Int, frameData: ByteArray): ByteArray {
        // ANMF data = 16 bytes header + frame data (+ 1 padding byte if data size is odd)
        val dataSize = 16 + frameData.size
        val needsPadding = dataSize % 2 != 0
        val totalSize = 4 + 4 + dataSize + (if (needsPadding) 1 else 0)

        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("ANMF".toByteArray())
        buf.putInt(dataSize)
        putUint24(buf, 0)           // Frame X / 2 = 0
        putUint24(buf, 0)           // Frame Y / 2 = 0
        putUint24(buf, width - 1)   // Frame Width Minus One
        putUint24(buf, height - 1)  // Frame Height Minus One
        putUint24(buf, delayMs)     // Frame Duration in ms (24-bit, max ~16.7s)
        buf.put(0)                   // flags: no blending, no disposal
        buf.put(frameData)
        if (needsPadding) buf.put(0)
        return buf.array()
    }

    private fun putUint24(buf: ByteBuffer, value: Int) {
        buf.put((value and 0xFF).toByte())
        buf.put(((value shr 8) and 0xFF).toByte())
        buf.put(((value shr 16) and 0xFF).toByte())
    }
}
