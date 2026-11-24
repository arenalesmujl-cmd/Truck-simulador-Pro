package com.example.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class WorldTextureSet(
    val whiteTex: Int,
    val asphaltTex: Int,
    val terrainTex: Int,
    val buildingTex: Int,
    val corrugatedTex: Int,
    val softShadowTex: Int,
    val glowDiscTex: Int,
    val driverFaceTex: Int,
    val driverClothTex: Int,
    val gaugeFaceTex: Int
) {
    fun release() {
        GLES30.glDeleteTextures(
            10,
            intArrayOf(
                whiteTex,
                asphaltTex,
                terrainTex,
                buildingTex,
                corrugatedTex,
                softShadowTex,
                glowDiscTex,
                driverFaceTex,
                driverClothTex,
                gaugeFaceTex
            ),
            0
        )
    }
}

object TextureFactory {

    fun createAll(): WorldTextureSet = WorldTextureSet(
        whiteTex = buildWhiteTexture(),
        asphaltTex = buildAsphaltTexture(),
        terrainTex = buildTerrainTexture(),
        buildingTex = buildBuildingFacadeTexture(),
        corrugatedTex = buildCorrugatedMetalTexture(),
        softShadowTex = buildSoftShadowTexture(),
        glowDiscTex = buildGlowDiscTexture(),
        driverFaceTex = buildDriverFaceTexture(),
        driverClothTex = buildDriverClothTexture(),
        gaugeFaceTex = buildGaugeFaceTexture()
    )

    private fun buildWhiteTexture(): Int {
        val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        return uploadTexture(bmp, repeat = true, mipmap = false)
    }

    private fun buildAsphaltTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        var seed = 88123
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }

        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val distTrack1 = abs(u - 0.27f)
                val distTrack2 = abs(u - 0.73f)
                val wear = (1f - (minOf(distTrack1, distTrack2) * 7.5f).coerceIn(0f, 1f)) * 0.12f
                val crack = if ((x + y * 3) % 193 == 0 && rand() > 0.55f) -0.10f else 0f
                val pebble = (rand() - 0.5f) * 0.14f
                val macro = sin(x * 0.045f) * cos(y * 0.038f) * 0.035f
                val base = (0.63f - wear + pebble + macro + crack).coerceIn(0.30f, 0.90f)
                val c = (base * 255f).toInt()
                pixels[y * size + x] = Color.argb(255, c, c, (c + 5).coerceAtMost(255))
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    private fun buildTerrainTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        var seed = 44219
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (y in 0 until size) {
            for (x in 0 until size) {
                val macro = sin(x * 0.032f) * cos(y * 0.032f) * 0.09f + sin(x * 0.09f + y * 0.07f) * 0.05f
                val blade = (rand() - 0.5f) * 0.11f
                val r = ((0.82f + macro + blade) * 255f).toInt().coerceIn(135, 255)
                val g = ((0.92f + macro + blade) * 255f).toInt().coerceIn(160, 255)
                val b = ((0.78f + macro + blade * 0.7f) * 255f).toInt().coerceIn(124, 255)
                pixels[y * size + x] = Color.argb(255, r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    private fun buildBuildingFacadeTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(122, 134, 150))

        val cols = 8
        val rows = 14
        val cellW = size.toFloat() / cols
        val cellH = size.toFloat() / rows
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val spandrelPaint = Paint().apply { color = Color.rgb(58, 66, 80) }
        val mullionPaint = Paint().apply {
            color = Color.rgb(42, 50, 62)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        val blindPaint = Paint().apply { color = Color.argb(42, 235, 242, 250) }

        var seed = 91827
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }

        for (r in 0 until rows) {
            canvas.drawRect(0f, r * cellH, size.toFloat(), r * cellH + 5f, spandrelPaint)
            for (c in 0 until cols) {
                val x0 = c * cellW + 4f
                val y0 = r * cellH + 6f
                val x1 = (c + 1) * cellW - 4f
                val y1 = (r + 1) * cellH - 3f
                val tint = rand()
                val topCol = when {
                    tint > 0.80f -> Color.rgb(166, 206, 238)
                    tint > 0.45f -> Color.rgb(98, 140, 176)
                    else -> Color.rgb(70, 98, 130)
                }
                val botCol = Color.rgb(32, 48, 72)
                glassPaint.shader = LinearGradient(x0, y0, x0, y1, topCol, botCol, Shader.TileMode.CLAMP)
                canvas.drawRect(x0, y0, x1, y1, glassPaint)
                if (tint > 0.58f) {
                    canvas.drawRect(x0 + 2f, y0 + 2f, x1 - 2f, y0 + (y1 - y0) * 0.35f, blindPaint)
                }
                canvas.drawRect(x0, y0, x1, y1, mullionPaint)
            }
        }
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    private fun buildCorrugatedMetalTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val rib = sin((x.toFloat() / size) * 2f * Math.PI.toFloat() * 18f) * 0.11f
                val seam = if (y % 128 < 3 || x % 256 < 3) -0.14f else 0f
                val rivet = if ((y % 128 in 6..9) && (x % 32 in 14..17)) 0.09f else 0f
                val v = ((0.88f + rib + seam + rivet) * 255f).toInt().coerceIn(140, 255)
                pixels[y * size + x] = Color.argb(255, v, v, v)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    private fun buildSoftShadowTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size * 0.5f, size * 0.5f, size * 0.5f,
                intArrayOf(
                    Color.argb(170, 6, 8, 12),
                    Color.argb(115, 6, 8, 12),
                    Color.argb(35, 6, 8, 12),
                    Color.argb(0, 6, 8, 12)
                ),
                floatArrayOf(0f, 0.45f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return uploadTexture(bmp, repeat = false, mipmap = false)
    }

    private fun buildGlowDiscTexture(): Int {
        val size = 128
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                size * 0.5f, size * 0.5f, size * 0.5f,
                intArrayOf(
                    Color.argb(255, 255, 255, 255),
                    Color.argb(210, 255, 245, 210),
                    Color.argb(70, 255, 215, 140),
                    Color.argb(0, 255, 190, 100)
                ),
                floatArrayOf(0f, 0.22f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        return uploadTexture(bmp, repeat = false, mipmap = false)
    }

    /**
     * High-resolution 512x512 cylindrical UV skin, eye, brow, nose contour, lip, beard-fade & hair texture
     * aligned ring-for-ring to CharacterMeshFactory.buildSculptedUvHeadMesh():
     *   u = 0.25 (x = 128px) -> center front of face (+Z)
     *   v = 0.267 (ri=4) -> chin bottom, v = 0.400 (ri=6) -> lower lip,
     *   v = 0.533 (ri=8) -> nose tip,   v = 0.600 (ri=9) -> orbital eye sockets,
     *   v = 0.667 (ri=10) -> supraorbital brow ridge, v = 0.733 (ri=11) -> cap rim
     */
    private fun buildDriverFaceTexture(): Int {
        val s = 512
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Base warm natural skin gradient from shaded neck up to sunlit forehead
        val skinBg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, s.toFloat(),
                intArrayOf(
                    Color.rgb(186, 130, 103), // neck base
                    Color.rgb(214, 158, 126), // jaw
                    Color.rgb(232, 180, 148), // mid-face / cheekbones
                    Color.rgb(226, 174, 141)  // forehead
                ),
                floatArrayOf(0.0f, 0.26f, 0.56f, 0.88f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), skinBg)

        // Subtle organic skin pores & micro-variation
        val porePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var seed = 31337
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (i in 0 until 2000) {
            val px = rand() * s
            val py = (0.18f + rand() * 0.65f) * s
            porePaint.color = if (i % 2 == 0) Color.argb(13, 162, 102, 76) else Color.argb(11, 250, 212, 186)
            canvas.drawCircle(px, py, 1.3f + rand() * 1.6f, porePaint)
        }

        // Front face center X = 0.25 * 512 = 128f
        val cx = s * 0.25f

        // Warm cheekbone highlights & healthy cheek flush
        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - 35f, s * 0.515f, 25f, Color.argb(42, 214, 120, 96), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx - 35f, s * 0.515f, 25f, cheekPaint)
        cheekPaint.shader = RadialGradient(cx + 35f, s * 0.515f, 25f, Color.argb(42, 214, 120, 96), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx + 35f, s * 0.515f, 25f, cheekPaint)

        // Sculpted nose dorsal highlight + soft alar crease + realistic nostrils (v = 0.485 .. 0.600)
        val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        nosePaint.shader = LinearGradient(cx, s * 0.50f, cx, s * 0.61f, Color.argb(38, 255, 232, 215), Color.argb(12, 255, 232, 215), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(RectF(cx - 5f, s * 0.50f, cx + 5f, s * 0.605f), 5f, 5f, nosePaint)
        nosePaint.shader = null
        // Subtle under-tip nose shadow + left/right nostrils at v = 0.492
        nosePaint.shader = RadialGradient(cx, s * 0.492f, 15f, Color.argb(72, 138, 86, 66), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 14f, s * 0.482f, cx + 14f, s * 0.505f), nosePaint)
        nosePaint.shader = null
        nosePaint.color = Color.argb(145, 92, 52, 42)
        canvas.drawOval(RectF(cx - 9.5f, s * 0.488f, cx - 3.2f, s * 0.498f), nosePaint)
        canvas.drawOval(RectF(cx + 3.2f, s * 0.488f, cx + 9.5f, s * 0.498f), nosePaint)

        // Subtle philtrum shadow above upper lip
        nosePaint.color = Color.argb(36, 150, 92, 70)
        canvas.drawRoundRect(RectF(cx - 4f, s * 0.455f, cx + 4f, s * 0.484f), 3f, 3f, nosePaint)

        // Well-groomed cropped jawline beard + sideburn fade (v = 0.22 .. 0.44)
        val beardBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, s * 0.315f, 66f,
                intArrayOf(
                    Color.argb(195, 44, 31, 24),
                    Color.argb(145, 54, 38, 30),
                    Color.argb(0, 54, 38, 30)
                ),
                floatArrayOf(0f, 0.64f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(cx - 72f, s * 0.195f, cx + 72f, s * 0.445f), beardBase)

        val bristlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.3f }
        for (i in 0 until 1500) {
            val angle = rand() * Math.PI.toFloat() * 2f
            val rad = sqrt(rand().toDouble()).toFloat()
            val bx = cx + cos(angle) * rad * 64f
            val by = s * 0.32f + sin(angle) * rad * 42f
            if (by > s * 0.405f && abs(bx - cx) < 28f) continue
            bristlePaint.color = if (i % 3 == 0) Color.argb(160, 32, 22, 17) else Color.argb(130, 64, 46, 34)
            canvas.drawLine(bx, by, bx + (rand() - 0.5f) * 2f, by - 4f, bristlePaint)
        }

        // Clean mustache cleanly framed below nostrils & above upper lip (v = 0.446 .. 0.478)
        val stachePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx, s * 0.446f, cx, s * 0.478f, Color.argb(175, 42, 30, 23), Color.argb(130, 42, 30, 23), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(cx - 22f, s * 0.446f, cx + 22f, s * 0.476f), 7f, 7f, stachePaint)

        // Natural Sculpted Lips (v = 0.408 .. 0.442)
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        lipPaint.color = Color.rgb(184, 118, 106)
        canvas.drawOval(RectF(cx - 16f, s * 0.408f, cx + 16f, s * 0.428f), lipPaint) // lower lip
        lipPaint.color = Color.rgb(158, 94, 86)
        canvas.drawOval(RectF(cx - 17f, s * 0.425f, cx + 17f, s * 0.441f), lipPaint) // upper lip
        lipPaint.color = Color.argb(165, 86, 44, 40)
        lipPaint.strokeWidth = 1.5f
        canvas.drawLine(cx - 16f, s * 0.426f, cx + 16f, s * 0.426f, lipPaint)

        // Expressive Photorealistic Eyes aligned 1:1 with sculpted orbital hollows (v = 0.600, dx = +-28px)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * 28.0f
            val ey = s * 0.600f

            // Orbital recess ambient shade
            p.shader = RadialGradient(ex, ey, 21f, Color.argb(68, 126, 78, 58), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawOval(RectF(ex - 20f, ey - 13f, ex + 20f, ey + 13f), p)
            p.shader = null

            // Almond sclera with subtle pink medial caruncle warmth
            p.color = Color.rgb(236, 234, 230)
            canvas.drawOval(RectF(ex - 11.5f, ey - 6.0f, ex + 11.5f, ey + 6.0f), p)
            p.color = Color.argb(85, 198, 128, 116)
            canvas.drawCircle(ex - side * 9.5f, ey, 2.2f, p)

            // Steel-blue / hazel iris + limbal ring + deep pupil + dual catchlight
            p.color = Color.rgb(30, 50, 64)
            canvas.drawCircle(ex, ey, 5.4f, p)
            p.color = Color.rgb(58, 108, 134)
            canvas.drawCircle(ex, ey, 4.4f, p)
            p.color = Color.rgb(86, 138, 162)
            canvas.drawCircle(ex, ey - 0.8f, 3.0f, p)
            p.color = Color.rgb(12, 14, 20)
            canvas.drawCircle(ex, ey, 2.2f, p)
            p.color = Color.argb(240, 255, 255, 255)
            canvas.drawCircle(ex + 1.6f, ey + 1.5f, 1.1f, p)
            p.color = Color.argb(135, 255, 255, 255)
            canvas.drawCircle(ex - 1.3f, ey - 1.4f, 0.6f, p)

            // Upper lash line & eyelid fold crease
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.5f
            p.color = Color.rgb(34, 24, 20)
            canvas.drawArc(RectF(ex - 12f, ey - 5f, ex + 12f, ey + 6.5f), 10f, 160f, false, p)
            p.strokeWidth = 1.3f
            p.color = Color.argb(125, 118, 74, 56)
            canvas.drawArc(RectF(ex - 12f, ey - 1f, ex + 12f, ey + 11.5f), 14f, 152f, false, p)
            p.style = Paint.Style.FILL

            // Natural textured arched eyebrow aligned directly onto supraorbital brow ridge (v = 0.658)
            p.color = Color.rgb(34, 25, 20)
            p.strokeWidth = 1.8f
            for (b in 0..24) {
                val frac = b / 24f
                val bx = ex - side * 12.5f + side * frac * 26f
                val arch = sin(frac * Math.PI).toFloat() * 4.0f
                val by = s * 0.652f + arch
                canvas.drawLine(bx, by - 2.2f, bx + side * 2.2f, by + 2.4f, p)
            }
        }

        // Crisp hairline across back/sides & top crown underneath cap
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        hairPaint.color = Color.rgb(28, 22, 17)
        canvas.drawRect(0f, s * 0.25f, s * 0.095f, s.toFloat(), hairPaint)
        canvas.drawRect(s * 0.405f, s * 0.25f, s.toFloat(), s.toFloat(), hairPaint)
        canvas.drawRect(0f, s * 0.74f, s.toFloat(), s.toFloat(), hairPaint)
        canvas.drawRect(s * 0.06f, s * 0.36f, s * 0.115f, s * 0.65f, hairPaint)
        canvas.drawRect(s * 0.385f, s * 0.36f, s * 0.44f, s * 0.65f, hairPaint)
        hairPaint.strokeWidth = 1.6f
        for (i in 0 until 480) {
            val hx = if (i % 2 == 0) s * (0.42f + rand() * 0.56f) else rand() * s * 0.09f
            val hy = s * (0.28f + rand() * 0.68f)
            hairPaint.color = if (i % 3 == 0) Color.argb(120, 52, 40, 32) else Color.argb(140, 20, 15, 12)
            canvas.drawLine(hx, hy, hx + (rand() - 0.5f) * 4f, hy + 14f, hairPaint)
        }

        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    /**
     * Subtle woven canvas, selvedge denim twill & leather grain micro-texture multiplied onto apparel.
     */
    private fun buildDriverClothTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        var seed = 77411
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (y in 0 until size) {
            for (x in 0 until size) {
                val twill = sin((x + y) * 0.78f) * 0.042f
                val weave = cos((x - y) * 0.78f) * 0.028f
                val fiber = (rand() - 0.5f) * 0.055f
                val v = ((0.94f + twill + weave + fiber) * 255f).toInt().coerceIn(195, 255)
                pixels[y * size + x] = Color.argb(255, v, v, v)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    /**
     * High-contrast illuminated radial gauge dial face for cockpit speedometer & tachometer.
     */
    private fun buildGaugeFaceTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size * 0.5f
        val cy = size * 0.5f

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, size * 0.5f,
                intArrayOf(Color.rgb(18, 26, 40), Color.rgb(10, 14, 22), Color.rgb(4, 7, 12)),
                floatArrayOf(0f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, size * 0.49f, bg)

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        for (i in 0..24) {
            val deg = 135f + (i / 24f) * 270f
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val major = i % 2 == 0
            val rIn = size * (if (major) 0.35f else 0.40f)
            val rOut = size * 0.45f
            tickPaint.color = if (i >= 20) Color.rgb(244, 63, 94) else if (major) Color.rgb(226, 232, 240) else Color.rgb(100, 116, 139)
            tickPaint.strokeWidth = if (major) 4f else 2f
            canvas.drawLine(
                cx + cos(rad) * rIn, cy + sin(rad) * rIn,
                cx + cos(rad) * rOut, cy + sin(rad) * rOut,
                tickPaint
            )
        }

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(140, 56, 189, 248)
        }
        canvas.drawArc(RectF(size * 0.14f, size * 0.14f, size * 0.86f, size * 0.86f), 135f, 270f, false, ringPaint)

        return uploadTexture(bmp, repeat = false, mipmap = true)
    }

    private fun uploadTexture(bmp: Bitmap, repeat: Boolean, mipmap: Boolean): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val texId = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        val wrap = if (repeat) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrap)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrap)
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            if (mipmap) GLES30.GL_LINEAR_MIPMAP_LINEAR else GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0)
        if (mipmap) {
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        bmp.recycle()
        return texId
    }
}
