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
                val wear = (1f - (minOf(distTrack1, distTrack2) * 7.2f).coerceIn(0f, 1f)) * 0.13f
                val tarSeam = if (y % 256 in 0..1 || (abs(x - size / 2) <= 1 && (y % 48) < 20)) -0.11f else 0f
                val crack = if ((x + y * 3) % 211 == 0 && rand() > 0.62f) -0.07f else 0f
                val pebble = (rand() - 0.5f) * 0.10f + (if (rand() > 0.92f) 0.055f else 0f)
                val macro = sin(x * 0.045f) * cos(y * 0.038f) * 0.028f
                val base = (0.64f - wear + pebble + macro + crack + tarSeam).coerceIn(0.28f, 0.90f)
                val c = (base * 255f).toInt()
                pixels[y * size + x] = Color.argb(255, c, c, (c + 6).coerceAtMost(255))
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
                val macro = sin(x * 0.032f) * cos(y * 0.032f) * 0.085f +
                    sin(x * 0.088f + y * 0.071f) * 0.048f +
                    cos(x * 0.018f - y * 0.021f) * 0.04f
                val blade = (rand() - 0.5f) * 0.095f
                val soilTint = if (macro < -0.04f) 0.04f else 0f
                val r = ((0.83f + macro + blade + soilTint) * 255f).toInt().coerceIn(140, 255)
                val g = ((0.93f + macro + blade) * 255f).toInt().coerceIn(164, 255)
                val b = ((0.78f + macro + blade * 0.7f) * 255f).toInt().coerceIn(126, 255)
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
                    tint > 0.84f -> Color.rgb(184, 218, 246)
                    tint > 0.56f -> Color.rgb(112, 154, 192)
                    tint > 0.25f -> Color.rgb(74, 106, 140)
                    else -> Color.rgb(52, 74, 102)
                }
                val botCol = if (tint > 0.84f) Color.rgb(78, 108, 138) else Color.rgb(26, 40, 62)
                glassPaint.shader = LinearGradient(x0, y0, x1, y1, topCol, botCol, Shader.TileMode.CLAMP)
                canvas.drawRect(x0, y0, x1, y1, glassPaint)
                if (tint > 0.55f) {
                    canvas.drawRect(x0 + 2f, y0 + 2f, x1 - 2f, y0 + (y1 - y0) * 0.32f, blindPaint)
                }
                // Crisp diagonal sky glint highlight across select curtain-wall panes
                if (tint > 0.72f) {
                    glassPaint.shader = null
                    glassPaint.color = Color.argb(38, 255, 255, 255)
                    canvas.drawLine(x0 + 4f, y1 - 6f, x1 - 5f, y0 + 5f, glassPaint)
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
     *
     * Note on cylindrical UV aspect ratio:
     *   Horizontal density ~0.96 px/mm (512px / 0.534m), vertical density ~1.86 px/mm (205px / 0.110m).
     *   All round facial elements use vertical factor yScale = 1.94f so irises, pupils, catchlights,
     *   and nostrils render genuinely circular on the 3D head instead of squashed horizontally!
     */
    /**
     * Ultra-sharp 1024x1024 cylindrical UV skin, iris, brow, nose contour, lip, beard-fade & hair texture
     * aligned ring-for-ring to CharacterMeshFactory.buildSculptedUvHeadMesh():
     *   u = 0.25 (x = 256px) -> center front of face (+Z)
     *   v = 0.267 (ri=4) -> chin bottom, v = 0.400 (ri=6) -> lower lip,
     *   v = 0.533 (ri=8) -> nose tip,   v = 0.600 (ri=9) -> orbital eye sockets,
     *   v = 0.667 (ri=10) -> supraorbital brow ridge, v = 0.733 (ri=11) -> cap rim
     */
    private fun buildDriverFaceTexture(): Int {
        val s = 1024
        val k = 2.0f // exact 2x scale relative to 512 baseline
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val yScale = 1.94f

        fun drawTrueCircle(cx: Float, cy: Float, rPx: Float, paint: Paint) {
            canvas.drawOval(RectF(cx - rPx, cy - rPx * yScale, cx + rPx, cy + rPx * yScale), paint)
        }

        // Warm natural sunlit complexion gradient matched cleanly to torso neck collar
        val skinBg = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, s.toFloat(),
                intArrayOf(
                    Color.rgb(198, 144, 114), // warm shaded neck base matching torso collar
                    Color.rgb(214, 158, 126), // jaw & chin
                    Color.rgb(233, 182, 149), // mid-face / cheekbones
                    Color.rgb(225, 173, 140)  // forehead
                ),
                floatArrayOf(0.0f, 0.24f, 0.55f, 0.88f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), skinBg)

        // Fine organic skin pores, freckles & micro-tonal variation
        val porePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var seed = 31337
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (i in 0 until 4400) {
            val px = rand() * s
            val py = (0.14f + rand() * 0.70f) * s
            porePaint.color = if (i % 2 == 0) Color.argb(11, 154, 94, 68) else Color.argb(10, 252, 214, 188)
            drawTrueCircle(px, py, (0.85f + rand() * 1.15f) * k, porePaint)
        }

        // Front face center X = 0.25 * 1024 = 256f
        val cx = s * 0.25f

        // Warm cheekbone vascular flush, subtle temple shadow & forehead expression lines
        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (side in listOf(-1f, 1f)) {
            cheekPaint.shader = RadialGradient(
                cx + side * 34f * k, s * 0.522f, 25f * k,
                Color.argb(42, 212, 112, 88), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawOval(
                RectF(
                    cx + side * 34f * k - 24f * k, s * 0.522f - 24f * k * 1.4f,
                    cx + side * 34f * k + 24f * k, s * 0.522f + 24f * k * 1.4f
                ),
                cheekPaint
            )
            // Soft nasolabial smile-line contour
            cheekPaint.shader = null
            cheekPaint.style = Paint.Style.STROKE
            cheekPaint.strokeWidth = 2.2f * k
            cheekPaint.color = Color.argb(26, 128, 74, 56)
            canvas.drawArc(
                RectF(cx + side * 12f * k - 12f * k, s * 0.405f, cx + side * 12f * k + 18f * k, s * 0.515f),
                if (side > 0f) 115f else 25f, 45f, false, cheekPaint
            )
            cheekPaint.style = Paint.Style.FILL
        }

        // Sculpted nose dorsal highlight, soft bridge shadow, alar crease & anatomical nostrils
        val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        nosePaint.shader = LinearGradient(
            cx, s * 0.495f, cx, s * 0.612f,
            Color.argb(52, 255, 236, 220), Color.argb(12, 255, 236, 220), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(cx - 4.2f * k, s * 0.495f, cx + 4.2f * k, s * 0.610f), 4.2f * k, 9f * k, nosePaint)
        nosePaint.shader = RadialGradient(cx, s * 0.494f, 16f * k, Color.argb(72, 132, 78, 58), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 14f * k, s * 0.474f, cx + 14f * k, s * 0.514f), nosePaint)
        nosePaint.shader = null
        nosePaint.color = Color.argb(165, 78, 42, 34)
        canvas.drawOval(RectF(cx - 8.4f * k, s * 0.483f, cx - 3.0f * k, s * 0.498f), nosePaint)
        canvas.drawOval(RectF(cx + 3.0f * k, s * 0.483f, cx + 8.4f * k, s * 0.498f), nosePaint)

        // Subtle philtrum groove
        nosePaint.color = Color.argb(36, 144, 86, 66)
        canvas.drawRoundRect(RectF(cx - 3.2f * k, s * 0.448f, cx + 3.2f * k, s * 0.482f), 3f * k, 6f * k, nosePaint)

        // Natural cropped beard + realistic gradient cheek fade + fine multi-tone beard follicles
        val beardBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, s * 0.300f, 84f * k,
                intArrayOf(
                    Color.argb(195, 38, 26, 20),
                    Color.argb(138, 48, 33, 25),
                    Color.argb(0, 48, 33, 25)
                ),
                floatArrayOf(0f, 0.58f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(cx - 68f * k, s * 0.135f, cx + 68f * k, s * 0.456f), beardBase)

        val bristlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.15f * k }
        for (i in 0 until 3400) {
            val angle = rand() * Math.PI.toFloat() * 2f
            val rad = sqrt(rand().toDouble()).toFloat()
            val bx = cx + cos(angle) * rad * 64f * k
            val by = s * 0.310f + sin(angle) * rad * 66f * k
            if (by > s * 0.388f && by < s * 0.445f && abs(bx - cx) < 24f * k) continue
            if (by > s * 0.450f && abs(bx - cx) > 22f * k) continue
            bristlePaint.color = when (i % 4) {
                0 -> Color.argb(155, 24, 17, 13)
                1 -> Color.argb(125, 58, 40, 29)
                else -> Color.argb(135, 36, 25, 19)
            }
            canvas.drawLine(bx, by, bx + (rand() - 0.5f) * 1.8f * k, by - 6.5f * k, bristlePaint)
        }

        // Contoured natural mustache hairs with clean Cupid's bow arch
        val stacheSoft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, s * 0.454f, 24f * k, Color.argb(145, 34, 24, 18), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawOval(RectF(cx - 24f * k, s * 0.436f, cx + 24f * k, s * 0.474f), stacheSoft)
        for (i in 0 until 260) {
            val mx = cx + (rand() - 0.5f) * 38f * k
            val my = s * (0.442f + rand() * 0.028f)
            bristlePaint.color = Color.argb(150, 28, 19, 14)
            canvas.drawLine(mx, my, mx + (mx - cx) * 0.08f, my - 4.5f * k, bristlePaint)
        }

        // Sculpted lips with crisp vermilion border, parting shade & natural lower-lip highlight
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        lipPaint.color = Color.rgb(186, 116, 104)
        canvas.drawOval(RectF(cx - 16.5f * k, s * 0.398f, cx + 16.5f * k, s * 0.426f), lipPaint)
        lipPaint.color = Color.rgb(156, 92, 84)
        canvas.drawOval(RectF(cx - 17.5f * k, s * 0.420f, cx + 17.5f * k, s * 0.443f), lipPaint)
        lipPaint.color = Color.argb(56, 255, 220, 210)
        canvas.drawOval(RectF(cx - 8.5f * k, s * 0.404f, cx + 8.5f * k, s * 0.417f), lipPaint)
        lipPaint.color = Color.argb(185, 76, 38, 34)
        lipPaint.strokeWidth = 1.9f * k
        canvas.drawLine(cx - 16.5f * k, s * 0.422f, cx + 16.5f * k, s * 0.422f, lipPaint)

        // Photorealistic 1024px Eyes with detailed iris stroma fibers, limbal ring, cornea depth & crisp lash creases
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * 27.5f * k
            val ey = s * 0.600f

            // Orbital socket AO shade
            p.shader = RadialGradient(ex, ey, 22f * k, Color.argb(68, 120, 72, 52), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawOval(RectF(ex - 20f * k, ey - 20f * k * yScale * 0.65f, ex + 20f * k, ey + 20f * k * yScale * 0.65f), p)
            p.shader = null

            // Almond sclera with shaded corners & subtle pink caruncle
            p.color = Color.rgb(236, 234, 231)
            canvas.drawOval(RectF(ex - 11.2f * k, ey - 5.9f * k * yScale, ex + 11.2f * k, ey + 5.9f * k * yScale), p)
            p.color = Color.argb(95, 196, 120, 108)
            drawTrueCircle(ex - side * 9.0f * k, ey, 1.85f * k, p)

            // Rich multi-layered hazel-steel iris + radial stroma fibers + deep pupil + sharp dual catchlights
            p.color = Color.rgb(22, 38, 52)
            drawTrueCircle(ex, ey, 5.15f * k, p) // limbal ring
            p.color = Color.rgb(48, 96, 126)
            drawTrueCircle(ex, ey, 4.35f * k, p)
            p.color = Color.rgb(88, 144, 174)
            drawTrueCircle(ex, ey - 0.7f * k * yScale, 2.95f * k, p)
            // Delicate iris radial fibers
            p.strokeWidth = 1.1f * k
            for (fi in 0 until 20) {
                val fa = (fi / 20f) * Math.PI.toFloat() * 2f
                p.color = if (fi % 2 == 0) Color.argb(115, 148, 198, 220) else Color.argb(90, 34, 68, 90)
                canvas.drawLine(
                    ex + cos(fa) * 2.1f * k, ey + sin(fa) * 2.1f * k * yScale,
                    ex + cos(fa) * 4.2f * k, ey + sin(fa) * 4.2f * k * yScale,
                    p
                )
            }
            p.color = Color.rgb(8, 10, 16)
            drawTrueCircle(ex, ey, 2.05f * k, p)
            p.color = Color.argb(250, 255, 255, 255)
            drawTrueCircle(ex + 1.45f * k, ey + 1.35f * k * yScale, 1.02f * k, p)
            p.color = Color.argb(155, 255, 255, 255)
            drawTrueCircle(ex - 1.25f * k, ey - 1.25f * k * yScale, 0.54f * k, p)

            // Upper lash line, upper eyelid crease & lower rim shadow
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3.0f * k
            p.color = Color.rgb(26, 18, 14)
            canvas.drawArc(RectF(ex - 11.4f * k, ey - 4.8f * k * yScale, ex + 11.4f * k, ey + 6.1f * k * yScale), 12f, 156f, false, p)
            p.strokeWidth = 1.5f * k
            p.color = Color.argb(132, 108, 66, 48)
            canvas.drawArc(RectF(ex - 11.5f * k, ey - 0.8f * k * yScale, ex + 11.5f * k, ey + 10.6f * k * yScale), 15f, 150f, false, p)
            p.style = Paint.Style.FILL

            // Natural crisp individual eyebrow hairs arched onto supraorbital ridge
            p.color = Color.rgb(32, 22, 16)
            p.strokeWidth = 1.7f * k
            for (b in 0..40) {
                val frac = b / 40f
                val bx = ex - side * 12.8f * k + side * frac * 26.5f * k
                val arch = sin(frac * Math.PI).toFloat() * 7.2f * k
                val by = s * 0.658f + arch
                canvas.drawLine(bx, by - 4.0f * k, bx + side * 2.1f * k, by + 4.2f * k, p)
            }
        }

        // Tapered natural hairline & feathered sideburns along back & temples
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        hairPaint.color = Color.rgb(24, 18, 14)
        canvas.drawRect(0f, s * 0.745f, s.toFloat(), s.toFloat(), hairPaint)
        canvas.drawRect(0f, s * 0.28f, s * 0.055f, s.toFloat(), hairPaint)
        canvas.drawRect(s * 0.445f, s * 0.28f, s.toFloat(), s.toFloat(), hairPaint)
        hairPaint.shader = LinearGradient(s * 0.050f, 0f, s * 0.112f, 0f, Color.argb(230, 24, 18, 14), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(s * 0.050f, s * 0.34f, s * 0.112f, s * 0.75f, hairPaint)
        hairPaint.shader = LinearGradient(s * 0.450f, 0f, s * 0.388f, 0f, Color.argb(230, 24, 18, 14), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(s * 0.388f, s * 0.34f, s * 0.450f, s * 0.75f, hairPaint)
        hairPaint.shader = null
        hairPaint.strokeWidth = 1.5f * k
        for (i in 0 until 950) {
            val hx = if (i % 2 == 0) s * (0.43f + rand() * 0.56f) else rand() * s * 0.088f
            val hy = s * (0.28f + rand() * 0.68f)
            hairPaint.color = if (i % 3 == 0) Color.argb(128, 52, 40, 32) else Color.argb(150, 18, 13, 10)
            canvas.drawLine(hx, hy, hx + (rand() - 0.5f) * 3.5f * k, hy + 18f * k, hairPaint)
        }

        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    /**
     * High-detail woven canvas, selvedge denim twill & leather micro-texture multiplied onto apparel.
     */
    private fun buildDriverClothTexture(): Int {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        var seed = 77411
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (y in 0 until size) {
            for (x in 0 until size) {
                val twill = sin((x + y) * 0.92f) * 0.026f
                val weave = cos((x - y) * 0.92f) * 0.016f
                val fold = sin(y * 0.075f + sin(x * 0.035f) * 1.4f) * 0.015f
                val fiber = (rand() - 0.5f) * 0.024f
                val v = ((0.965f + twill + weave + fold + fiber) * 255f).toInt().coerceIn(220, 255)
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
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = size * 0.5f
        val cy = size * 0.5f

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, cy, size * 0.5f,
                intArrayOf(Color.rgb(22, 34, 54), Color.rgb(10, 16, 26), Color.rgb(3, 6, 11)),
                floatArrayOf(0f, 0.76f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, size * 0.49f, bg)

        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
        for (i in 0..48) {
            val deg = 135f + (i / 48f) * 270f
            val rad = Math.toRadians(deg.toDouble()).toFloat()
            val major = i % 4 == 0
            val medium = i % 2 == 0 && !major
            val rIn = size * (if (major) 0.34f else if (medium) 0.38f else 0.415f)
            val rOut = size * 0.455f
            tickPaint.color = if (i >= 40) Color.rgb(248, 70, 96) else if (major) Color.rgb(241, 245, 249) else Color.rgb(110, 128, 152)
            tickPaint.strokeWidth = if (major) 6f else if (medium) 3.5f else 2f
            canvas.drawLine(
                cx + cos(rad) * rIn, cy + sin(rad) * rIn,
                cx + cos(rad) * rOut, cy + sin(rad) * rOut,
                tickPaint
            )
        }

        // Crisp illuminated cyan accent arc & inner hub bezel
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4.5f
            color = Color.argb(175, 56, 189, 248)
        }
        canvas.drawArc(RectF(size * 0.12f, size * 0.12f, size * 0.88f, size * 0.88f), 135f, 225f, false, ringPaint)
        ringPaint.color = Color.argb(210, 244, 63, 94)
        canvas.drawArc(RectF(size * 0.12f, size * 0.12f, size * 0.88f, size * 0.88f), 360f, 45f, false, ringPaint)
        ringPaint.strokeWidth = 2.5f
        ringPaint.color = Color.argb(90, 148, 163, 184)
        canvas.drawCircle(cx, cy, size * 0.21f, ringPaint)

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
