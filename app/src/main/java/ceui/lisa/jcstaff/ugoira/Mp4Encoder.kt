package ceui.lisa.jcstaff.ugoira

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/**
 * Encodes a list of image frames into an H.264 MP4 file using MediaCodec + MediaMuxer.
 * Supports variable per-frame delays as specified by Ugoira metadata.
 */
object Mp4Encoder {

    fun encode(
        frames: List<Pair<File, Int>>,  // File + delay in milliseconds
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ) {
        if (frames.isEmpty()) return

        val firstBitmap = BitmapFactory.decodeFile(frames[0].first.absolutePath) ?: return
        val origWidth = firstBitmap.width
        val origHeight = firstBitmap.height
        firstBitmap.recycle()

        // H.264 requires dimensions that are multiples of 2
        val width = origWidth and 1.inv()
        val height = origHeight and 1.inv()
        val bitrate = (width * height * 2).coerceIn(500_000, 8_000_000)

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
            )
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufferInfo = MediaCodec.BufferInfo()
        var trackIndex = -1
        var muxerStarted = false

        var ptsUs = 0L
        var frameIndex = 0
        var inputDone = false
        var outputDone = false

        try {
            while (!outputDone) {
                if (!inputDone) {
                    val inputIdx = codec.dequeueInputBuffer(10_000L)
                    if (inputIdx >= 0) {
                        if (frameIndex < frames.size) {
                            val (file, delayMs) = frames[frameIndex]
                            val bitmap = loadAndScaleBitmap(file, width, height)
                            if (bitmap != null) {
                                val inputBuffer = codec.getInputBuffer(inputIdx)!!
                                val nv12 = bitmapToNV12(bitmap, width, height)
                                inputBuffer.clear()
                                inputBuffer.put(nv12)
                                bitmap.recycle()
                                codec.queueInputBuffer(inputIdx, 0, nv12.size, ptsUs, 0)
                            } else {
                                codec.queueInputBuffer(inputIdx, 0, 0, ptsUs, 0)
                            }
                            ptsUs += delayMs * 1000L
                            onProgress(frameIndex * 100 / frames.size)
                            frameIndex++
                        } else {
                            codec.queueInputBuffer(inputIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        }
                    }
                }

                val outputIdx = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                when {
                    outputIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    outputIdx >= 0 -> {
                        val isConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                        val isEos = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        if (!isConfig && bufferInfo.size > 0 && muxerStarted) {
                            val outputBuffer = codec.getOutputBuffer(outputIdx)!!
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputIdx, false)
                        if (isEos) outputDone = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            if (muxerStarted) muxer.stop()
            muxer.release()
        }
    }

    private fun loadAndScaleBitmap(file: File, width: Int, height: Int): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return if (bitmap.width == width && bitmap.height == height) {
            bitmap
        } else {
            val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
            bitmap.recycle()
            scaled
        }
    }

    /**
     * Convert ARGB bitmap to NV12 (YUV420SemiPlanar).
     * NV12: Y plane (width*height) followed by interleaved UV plane (width*height/2).
     */
    private fun bitmapToNV12(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val nv12 = ByteArray(width * height * 3 / 2)

        // Y plane
        for (i in 0 until height) {
            for (j in 0 until width) {
                val p = argb[i * width + j]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                nv12[i * width + j] = y.coerceIn(16, 235).toByte()
            }
        }

        // UV plane (NV12: interleaved U then V, 2×2 subsampled)
        val uvOffset = width * height
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val p = argb[row * 2 * width + col * 2]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                nv12[uvOffset + row * width + col * 2] = u.coerceIn(16, 240).toByte()
                nv12[uvOffset + row * width + col * 2 + 1] = v.coerceIn(16, 240).toByte()
            }
        }

        return nv12
    }
}
