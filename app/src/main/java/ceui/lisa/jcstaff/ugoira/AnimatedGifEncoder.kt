package ceui.lisa.jcstaff.ugoira

import android.graphics.Bitmap
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Animated GIF encoder based on NeuQuant algorithm.
 * Adapted from Kevin Weiner's AnimatedGifEncoder (https://www.java2s.com/Code/Java/2D-Graphics-GUI/AnimatedGifEncoder.htm)
 */
class AnimatedGifEncoder {
    private var width = 0
    private var height = 0
    private var transparent: Int? = null
    private var transIndex = 0
    private var repeat = 0
    private var delay = 0
    private var started = false
    private var out: OutputStream? = null
    private var image: Bitmap? = null
    private var pixels: ByteArray? = null
    private var indexedPixels: ByteArray? = null
    private var colorDepth = 0
    private var colorTab: ByteArray? = null
    private var usedEntry = BooleanArray(256)
    private var palSize = 7
    private var dispose = -1
    private var closeStream = false
    private var firstFrame = true
    private var sizeSet = false
    private var sample = 10

    /**
     * Sets the delay time between each frame, or changes it for subsequent frames
     */
    fun setDelay(ms: Int) {
        delay = ms / 10
    }

    /**
     * Sets the number of times the set of GIF frames should be played.
     * 0 = repeat indefinitely
     */
    fun setRepeat(iter: Int) {
        repeat = if (iter >= 0) iter else 0
    }

    /**
     * Sets the transparent color
     */
    fun setTransparent(c: Int) {
        transparent = c
    }

    /**
     * Sets frame disposal method (not commonly needed)
     */
    fun setDispose(code: Int) {
        if (code >= 0) dispose = code
    }

    /**
     * Sets quality of color quantization (1 = best, 10 = default, 20+ = fastest)
     */
    fun setQuality(quality: Int) {
        sample = if (quality < 1) 1 else quality
    }

    /**
     * Sets the GIF frame size
     */
    fun setSize(w: Int, h: Int) {
        if (!started || firstFrame) {
            width = w
            height = h
            if (width < 1) width = 320
            if (height < 1) height = 240
            sizeSet = true
        }
    }

    /**
     * Initiates GIF file creation on the given stream
     */
    fun start(os: OutputStream): Boolean {
        var ok = true
        closeStream = false
        out = os
        try {
            writeString("GIF89a")
        } catch (e: IOException) {
            ok = false
        }
        return ok.also { started = it }
    }

    /**
     * Initiates writing of a GIF file with the specified name
     */
    fun start(file: String): Boolean {
        var ok = true
        try {
            out = BufferedOutputStream(FileOutputStream(file))
            ok = start(out!!)
            closeStream = true
        } catch (e: IOException) {
            ok = false
        }
        return ok.also { started = it }
    }

    /**
     * Adds next GIF frame
     */
    fun addFrame(im: Bitmap): Boolean {
        if (!started || im == null) return false

        var ok = true
        try {
            if (!sizeSet) {
                setSize(im.width, im.height)
            }
            image = im
            getImagePixels()
            analyzePixels()
            if (firstFrame) {
                writeLSD()
                writePalette()
                if (repeat >= 0) {
                    writeNetscapeExt()
                }
            }
            writeGraphicCtrlExt()
            writeImageDesc()
            if (!firstFrame) {
                writePalette()
            }
            writePixels()
            firstFrame = false
        } catch (e: IOException) {
            ok = false
        }
        return ok
    }

    /**
     * Flushes any pending data and closes output stream
     */
    fun finish(): Boolean {
        if (!started) return false

        var ok = true
        started = false
        try {
            out?.write(0x3b)
            out?.flush()
            if (closeStream) {
                out?.close()
            }
        } catch (e: IOException) {
            ok = false
        }

        transIndex = 0
        out = null
        image = null
        pixels = null
        indexedPixels = null
        colorTab = null
        closeStream = false
        firstFrame = true

        return ok
    }

    private fun analyzePixels() {
        val len = pixels!!.size
        val nPix = len / 3
        indexedPixels = ByteArray(nPix)

        val nq = NeuQuant(pixels!!, len, sample)
        colorTab = nq.process()

        for (i in 0 until colorTab!!.size step 3) {
            val temp = colorTab!![i]
            colorTab!![i] = colorTab!![i + 2]
            colorTab!![i + 2] = temp
            usedEntry[i / 3] = false
        }

        var k = 0
        for (i in 0 until nPix) {
            val index = nq.map(
                pixels!![k++].toInt() and 0xff,
                pixels!![k++].toInt() and 0xff,
                pixels!![k++].toInt() and 0xff
            )
            usedEntry[index] = true
            indexedPixels!![i] = index.toByte()
        }
        pixels = null
        colorDepth = 8
        palSize = 7

        transparent?.let { c ->
            transIndex = findClosest(c)
        }
    }

    private fun findClosest(c: Int): Int {
        if (colorTab == null) return -1

        val r = (c shr 16) and 0xff
        val g = (c shr 8) and 0xff
        val b = c and 0xff

        var minpos = 0
        var dmin = 256 * 256 * 256

        val len = colorTab!!.size
        for (i in 0 until len step 3) {
            val dr = r - (colorTab!![i].toInt() and 0xff)
            val dg = g - (colorTab!![i + 1].toInt() and 0xff)
            val db = b - (colorTab!![i + 2].toInt() and 0xff)
            val d = dr * dr + dg * dg + db * db
            val index = i / 3
            if (usedEntry[index] && d < dmin) {
                dmin = d
                minpos = index
            }
        }
        return minpos
    }

    private fun getImagePixels() {
        val w = image!!.width
        val h = image!!.height

        if (w != width || h != height) {
            val temp = Bitmap.createScaledBitmap(image!!, width, height, true)
            image = temp
        }

        pixels = ByteArray(3 * width * height)
        val data = IntArray(width * height)
        image!!.getPixels(data, 0, width, 0, 0, width, height)

        for (i in data.indices) {
            val px = data[i]
            val j = i * 3
            // Store as BGR order (NeuQuant expects BGR)
            pixels!![j] = (px and 0xff).toByte()           // B
            pixels!![j + 1] = ((px shr 8) and 0xff).toByte()  // G
            pixels!![j + 2] = ((px shr 16) and 0xff).toByte() // R
        }
    }

    private fun writeGraphicCtrlExt() {
        out?.write(0x21)
        out?.write(0xf9)
        out?.write(4)

        var transp = 0
        var disp = if (dispose >= 0) dispose and 7 else 0
        if (transparent != null) {
            transp = 1
            disp = 2
        }
        disp = disp shl 2

        out?.write(disp or transp)
        writeShort(delay)
        out?.write(transIndex)
        out?.write(0)
    }

    private fun writeImageDesc() {
        out?.write(0x2c)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)

        if (firstFrame) {
            out?.write(0)
        } else {
            out?.write(0x80 or palSize)
        }
    }

    private fun writeLSD() {
        writeShort(width)
        writeShort(height)
        out?.write(0x80 or 0x70 or palSize)
        out?.write(0)
        out?.write(0)
    }

    private fun writeNetscapeExt() {
        out?.write(0x21)
        out?.write(0xff)
        out?.write(11)
        writeString("NETSCAPE2.0")
        out?.write(3)
        out?.write(1)
        writeShort(repeat)
        out?.write(0)
    }

    private fun writePalette() {
        out?.write(colorTab!!, 0, colorTab!!.size)
        val n = 3 * 256 - colorTab!!.size
        for (i in 0 until n) {
            out?.write(0)
        }
    }

    private fun writePixels() {
        val encoder = LZWEncoder(width, height, indexedPixels!!, colorDepth)
        encoder.encode(out!!)
    }

    private fun writeShort(value: Int) {
        out?.write(value and 0xff)
        out?.write((value shr 8) and 0xff)
    }

    private fun writeString(s: String) {
        for (c in s) {
            out?.write(c.code)
        }
    }
}

/**
 * NeuQuant Neural-Net Quantization Algorithm
 */
private class NeuQuant(
    private val thepicture: ByteArray,
    private val lengthcount: Int,
    private val samplefac: Int
) {

    companion object {
        private const val netsize = 256
        private const val prime1 = 499
        private const val prime2 = 491
        private const val prime3 = 487
        private const val prime4 = 503
        private const val minpicturebytes = 3 * prime4
        private const val maxnetpos = netsize - 1
        private const val netbiasshift = 4
        private const val ncycles = 100
        private const val intbiasshift = 16
        private const val intbias = 1 shl intbiasshift
        private const val gammashift = 10
        private const val gamma = 1 shl gammashift
        private const val betashift = 10
        private const val beta = intbias shr betashift
        private const val betagamma = intbias shl (gammashift - betashift)
        private const val initrad = netsize shr 3
        private const val radiusbiasshift = 6
        private const val radiusbias = 1 shl radiusbiasshift
        private const val initradius = initrad * radiusbias
        private const val radiusdec = 30
        private const val alphabiasshift = 10
        private const val initalpha = 1 shl alphabiasshift
        private const val radbiasshift = 8
        private const val radbias = 1 shl radbiasshift
        private const val alpharadbshift = alphabiasshift + radbiasshift
        private const val alpharadbias = 1 shl alpharadbshift
    }

    private var alphadec = 0
    private val network = Array(netsize) { IntArray(4) }
    private val netindex = IntArray(256)
    private val bias = IntArray(netsize)
    private val freq = IntArray(netsize)
    private val radpower = IntArray(initrad)

    init {
        for (i in 0 until netsize) {
            val p = network[i]
            p[2] = (i shl (netbiasshift + 8)) / netsize
            p[1] = p[2]
            p[0] = p[1]
            freq[i] = intbias / netsize
            bias[i] = 0
        }
    }

    fun process(): ByteArray {
        learn()
        unbiasnet()
        inxbuild()
        return colorMap()
    }

    private fun colorMap(): ByteArray {
        val map = ByteArray(3 * netsize)
        val index = IntArray(netsize)
        for (i in 0 until netsize) {
            index[network[i][3]] = i
        }
        var k = 0
        for (i in 0 until netsize) {
            val j = index[i]
            map[k++] = network[j][0].toByte()
            map[k++] = network[j][1].toByte()
            map[k++] = network[j][2].toByte()
        }
        return map
    }

    private fun inxbuild() {
        var previouscol = 0
        var startpos = 0
        for (i in 0 until netsize) {
            val p = network[i]
            var smallpos = i
            var smallval = p[1]
            for (j in i + 1 until netsize) {
                val q = network[j]
                if (q[1] < smallval) {
                    smallpos = j
                    smallval = q[1]
                }
            }
            val q = network[smallpos]
            if (i != smallpos) {
                var j = q[0]; q[0] = p[0]; p[0] = j
                j = q[1]; q[1] = p[1]; p[1] = j
                j = q[2]; q[2] = p[2]; p[2] = j
                j = q[3]; q[3] = p[3]; p[3] = j
            }
            if (smallval != previouscol) {
                netindex[previouscol] = (startpos + i) shr 1
                for (j in previouscol + 1 until smallval) {
                    netindex[j] = i
                }
                previouscol = smallval
                startpos = i
            }
        }
        netindex[previouscol] = (startpos + maxnetpos) shr 1
        for (j in previouscol + 1 until 256) {
            netindex[j] = maxnetpos
        }
    }

    private fun learn() {
        if (lengthcount < minpicturebytes) {
            samplefac.let { }
            return
        }

        alphadec = 30 + (samplefac - 1) / 3
        var p = thepicture
        var pix = 0
        val lim = lengthcount
        val samplepixels = lengthcount / (3 * samplefac)
        var delta = samplepixels / ncycles
        var alpha = initalpha
        var radius = initradius

        var rad = radius shr radiusbiasshift
        if (rad <= 1) rad = 0
        for (i in 0 until rad) {
            radpower[i] = alpha * (((rad * rad - i * i) * radbias) / (rad * rad))
        }

        val step = when {
            lengthcount < minpicturebytes -> 3
            lengthcount % prime1 != 0 -> 3 * prime1
            lengthcount % prime2 != 0 -> 3 * prime2
            lengthcount % prime3 != 0 -> 3 * prime3
            else -> 3 * prime4
        }

        var i = 0
        while (i < samplepixels) {
            val b = (p[pix].toInt() and 0xff) shl netbiasshift
            val g = (p[pix + 1].toInt() and 0xff) shl netbiasshift
            val r = (p[pix + 2].toInt() and 0xff) shl netbiasshift
            var j = contest(b, g, r)

            altersingle(alpha, j, b, g, r)
            if (rad != 0) alterneigh(rad, j, b, g, r)

            pix += step
            if (pix >= lim) pix -= lengthcount

            i++
            if (delta == 0) delta = 1
            if (i % delta == 0) {
                alpha -= alpha / alphadec
                radius -= radius / radiusdec
                rad = radius shr radiusbiasshift
                if (rad <= 1) rad = 0
                for (k in 0 until rad) {
                    radpower[k] = alpha * (((rad * rad - k * k) * radbias) / (rad * rad))
                }
            }
        }
    }

    fun map(b: Int, g: Int, r: Int): Int {
        var bestd = 1000
        var best = -1
        var i = netindex[g]
        var j = i - 1

        while (i < netsize || j >= 0) {
            if (i < netsize) {
                val p = network[i]
                var dist = p[1] - g
                if (dist >= bestd) {
                    i = netsize
                } else {
                    i++
                    if (dist < 0) dist = -dist
                    var a = p[0] - b
                    if (a < 0) a = -a
                    dist += a
                    if (dist < bestd) {
                        a = p[2] - r
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) {
                            bestd = dist
                            best = p[3]
                        }
                    }
                }
            }
            if (j >= 0) {
                val p = network[j]
                var dist = g - p[1]
                if (dist >= bestd) {
                    j = -1
                } else {
                    j--
                    if (dist < 0) dist = -dist
                    var a = p[0] - b
                    if (a < 0) a = -a
                    dist += a
                    if (dist < bestd) {
                        a = p[2] - r
                        if (a < 0) a = -a
                        dist += a
                        if (dist < bestd) {
                            bestd = dist
                            best = p[3]
                        }
                    }
                }
            }
        }
        return best
    }

    private fun unbiasnet() {
        for (i in 0 until netsize) {
            network[i][0] = network[i][0] shr netbiasshift
            network[i][1] = network[i][1] shr netbiasshift
            network[i][2] = network[i][2] shr netbiasshift
            network[i][3] = i
        }
    }

    private fun alterneigh(rad: Int, i: Int, b: Int, g: Int, r: Int) {
        var lo = i - rad
        if (lo < -1) lo = -1
        var hi = i + rad
        if (hi > netsize) hi = netsize

        var j = i + 1
        var k = i - 1
        var m = 1
        while (j < hi || k > lo) {
            val a = radpower[m++]
            if (j < hi) {
                val p = network[j++]
                p[0] -= (a * (p[0] - b)) / alpharadbias
                p[1] -= (a * (p[1] - g)) / alpharadbias
                p[2] -= (a * (p[2] - r)) / alpharadbias
            }
            if (k > lo) {
                val p = network[k--]
                p[0] -= (a * (p[0] - b)) / alpharadbias
                p[1] -= (a * (p[1] - g)) / alpharadbias
                p[2] -= (a * (p[2] - r)) / alpharadbias
            }
        }
    }

    private fun altersingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
        val n = network[i]
        n[0] -= (alpha * (n[0] - b)) / initalpha
        n[1] -= (alpha * (n[1] - g)) / initalpha
        n[2] -= (alpha * (n[2] - r)) / initalpha
    }

    private fun contest(b: Int, g: Int, r: Int): Int {
        var bestd = Int.MAX_VALUE
        var bestbiasd = Int.MAX_VALUE
        var bestpos = 0
        var bestbiaspos = 0

        for (i in 0 until netsize) {
            val n = network[i]
            var dist = n[0] - b
            if (dist < 0) dist = -dist
            var a = n[1] - g
            if (a < 0) a = -a
            dist += a
            a = n[2] - r
            if (a < 0) a = -a
            dist += a
            if (dist < bestd) {
                bestd = dist
                bestpos = i
            }
            val biasdist = dist - (bias[i] shr (intbiasshift - netbiasshift))
            if (biasdist < bestbiasd) {
                bestbiasd = biasdist
                bestbiaspos = i
            }
            val betafreq = freq[i] shr betashift
            freq[i] -= betafreq
            bias[i] += betafreq shl gammashift
        }
        freq[bestpos] += beta
        bias[bestpos] -= betagamma
        return bestbiaspos
    }
}

/**
 * LZW encoder for GIF
 */
private class LZWEncoder(
    private val imgW: Int,
    private val imgH: Int,
    private val pixAry: ByteArray,
    private val initCodeSize: Int
) {

    companion object {
        private const val EOF = -1
        private const val BITS = 12
        private const val HSIZE = 5003
    }

    private var curPixel = 0
    private var nBits = 0
    private var initBits = 0
    private var curAccum = 0
    private var curBits = 0
    private var masks = intArrayOf(
        0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF,
        0x01FF, 0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
    )
    private var aCount = 0
    private val accum = ByteArray(256)
    private val htab = IntArray(HSIZE)
    private val codetab = IntArray(HSIZE)

    private var freeEnt = 0
    private var clearFlg = false
    private var gInitBits = 0
    private var clearCode = 0
    private var eofCode = 0
    private var maxcode = 0

    fun encode(os: OutputStream) {
        os.write(initCodeSize)
        curPixel = 0
        compress(initCodeSize + 1, os)
        os.write(0)
    }

    private fun compress(initBits: Int, outs: OutputStream) {
        gInitBits = initBits
        clearFlg = false
        nBits = gInitBits
        maxcode = maxCode(nBits)

        clearCode = 1 shl (initBits - 1)
        eofCode = clearCode + 1
        freeEnt = clearCode + 2

        aCount = 0

        var ent = nextPixel()

        var hshift = 0
        var fcode = HSIZE
        while (fcode < 65536) {
            hshift++
            fcode *= 2
        }
        hshift = 8 - hshift

        val hSizeReg = HSIZE
        clHash(hSizeReg)

        output(clearCode, outs)

        var c: Int
        outerLoop@ while (nextPixel().also { c = it } != EOF) {
            fcode = (c shl BITS) + ent
            var i = (c shl hshift) xor ent

            if (htab[i] == fcode) {
                ent = codetab[i]
                continue
            } else if (htab[i] >= 0) {
                var disp = hSizeReg - i
                if (i == 0) disp = 1
                do {
                    i -= disp
                    if (i < 0) i += hSizeReg
                    if (htab[i] == fcode) {
                        ent = codetab[i]
                        continue@outerLoop
                    }
                } while (htab[i] >= 0)
            }
            output(ent, outs)
            ent = c
            if (freeEnt < (1 shl BITS)) {
                codetab[i] = freeEnt++
                htab[i] = fcode
            } else {
                clBlock(outs)
            }
        }
        output(ent, outs)
        output(eofCode, outs)
    }

    private fun output(code: Int, outs: OutputStream) {
        curAccum = curAccum and masks[curBits]

        curAccum = if (curBits > 0) {
            curAccum or (code shl curBits)
        } else {
            code
        }
        curBits += nBits

        while (curBits >= 8) {
            charOut((curAccum and 0xff).toByte(), outs)
            curAccum = curAccum shr 8
            curBits -= 8
        }

        if (freeEnt > maxcode || clearFlg) {
            if (clearFlg) {
                maxcode = maxCode(gInitBits.also { nBits = it })
                clearFlg = false
            } else {
                nBits++
                maxcode = if (nBits == BITS) {
                    1 shl BITS
                } else {
                    maxCode(nBits)
                }
            }
        }

        if (code == eofCode) {
            while (curBits > 0) {
                charOut((curAccum and 0xff).toByte(), outs)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            flushChar(outs)
        }
    }

    private fun maxCode(nBits: Int): Int {
        return (1 shl nBits) - 1
    }

    private fun clBlock(outs: OutputStream) {
        clHash(HSIZE)
        freeEnt = clearCode + 2
        clearFlg = true
        output(clearCode, outs)
    }

    private fun clHash(hSize: Int) {
        for (i in 0 until hSize) {
            htab[i] = -1
        }
    }

    private fun nextPixel(): Int {
        if (curPixel < pixAry.size) {
            return pixAry[curPixel++].toInt() and 0xff
        }
        return EOF
    }

    private fun charOut(c: Byte, outs: OutputStream) {
        accum[aCount++] = c
        if (aCount >= 254) {
            flushChar(outs)
        }
    }

    private fun flushChar(outs: OutputStream) {
        if (aCount > 0) {
            outs.write(aCount)
            outs.write(accum, 0, aCount)
            aCount = 0
        }
    }
}
