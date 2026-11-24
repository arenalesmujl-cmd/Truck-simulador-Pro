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

class WorldTextureSet(
    val whiteTex: Int,
    val asphaltTex: Int,
    val terrainTex: Int,
    val buildingTex: Int,
    val corrugatedTex: Int,
    val softShadowTex: Int,
    val glowDiscTex: Int,
    val driverFaceTex: Int,
    val driverClothTex: Int
) {
    fun release() {
        GLES30.glDeleteTextures(
            9,
            intArrayOf(
                whiteTex,
                asphaltTex,
                terrainTex,
                buildingTex,
                corrugatedTex,
                softShadowTex,
                glowDiscTex,
                driverFaceTex,
                driverClothTex
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
        driverClothTex = buildDriverClothTexture()
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
                val wear = (1f - (minOf(distTrack1, distTrack2) * 7.5f).coerceIn(0f, 1f)) * 0.11f
                val crack = if ((x + y * 3) % 193 == 0 && rand() > 0.55f) -0.09f else 0f
                val grain = (rand() - 0.5f) * 0.12f
                val macro = sin(x * 0.045f) * cos(y * 0.038f) * 0.035f
                val base = (0.64f - wear + grain + macro + crack).coerceIn(0.32f, 0.90f)
                val c = (base * 255f).toInt()
                pixels[y * size + x] = Color.argb(255, c, c, (c + 4).coerceAtMost(255))
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
                val macro = sin(x * 0.032f) * cos(y * 0.032f) * 0.08f + sin(x * 0.09f + y * 0.07f) * 0.045f
                val blade = (rand() - 0.5f) * 0.09f
                val r = ((0.82f + macro + blade) * 255f).toInt().coerceIn(140, 255)
                val g = ((0.92f + macro + blade) * 255f).toInt().coerceIn(165, 255)
                val b = ((0.78f + macro + blade * 0.7f) * 255f).toInt().coerceIn(128, 255)
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
        canvas.drawColor(Color.rgb(132, 142, 156))

        val cols = 8
        val rows = 14
        val cellW = size.toFloat() / cols
        val cellH = size.toFloat() / rows
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val spandrelPaint = Paint().apply { color = Color.rgb(66, 74, 88) }
        val mullionPaint = Paint().apply {
            color = Color.rgb(48, 56, 68)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }

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
                    tint > 0.80f -> Color.rgb(170, 204, 232)
                    tint > 0.45f -> Color.rgb(104, 144, 178)
                    else -> Color.rgb(74, 102, 134)
                }
                val botCol = Color.rgb(34, 50, 74)
                glassPaint.shader = LinearGradient(x0, y0, x0, y1, topCol, botCol, Shader.TileMode.CLAMP)
                canvas.drawRect(x0, y0, x1, y1, glassPaint)
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
                val seam = if (y % 128 < 3 || x % 256 < 3) -0.13f else 0f
                val rivet = if ((y % 128 in 6..9) && (x % 32 in 14..17)) 0.08f else 0f
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
     * High-resolution 512x512 cylindrical UV skin, eye, brow, lip, beard-fade & hair texture
     * aligned precisely with CharacterMeshFactory's lofted head rings:
     *   u = 0.25 -> center front of face (+Z)
     *   u = 0.00 / 0.50 -> ears/sides (+X / -X)
     *   u = 0.75 -> back of neck/head (-Z)
     *   v = 0.00 -> base of neck (-0.028m), v = 1.00 -> crown (+0.252m)
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
                    Color.rgb(184, 128, 102), // neck underside shadow
                    Color.rgb(214, 158, 126), // jaw
                    Color.rgb(230, 178, 145), // cheeks/face
                    Color.rgb(225, 172, 138)  // forehead
                ),
                floatArrayOf(0.0f, 0.28f, 0.58f, 0.92f),
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
        for (i in 0 until 1800) {
            val px = rand() * s
            val py = (0.20f + rand() * 0.65f) * s
            porePaint.color = if (i % 2 == 0) Color.argb(14, 165, 104, 78) else Color.argb(12, 250, 210, 185)
            canvas.drawCircle(px, py, 1.4f + rand() * 1.8f, porePaint)
        }

        // Front face center X = 0.25 * 512 = 128f
        val cx = s * 0.25f

        // Warm cheek flush & nose-bridge highlight
        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx - 32f, s * 0.56f, 26f, Color.argb(46, 212, 118, 96), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(cx - 32f, s * 0.56f, 26f, cheekPaint)
        cheekPaint.shader = RadialGradient(cx + 32f, s * 0.56f, 26f, Color.argb(46, 212, 118, 96), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx + 32f, s * 0.56f, 26f, cheekPaint)

        // Realistic cropped trimmed jaw beard + sideburns (smooth gradient + natural hair stippling, front face only!)
        val beardBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, s * 0.34f, 68f,
                intArrayOf(
                    Color.argb(200, 48, 34, 26),
                    Color.argb(150, 56, 40, 31),
                    Color.argb(0, 56, 40, 31)
                ),
                floatArrayOf(0f, 0.66f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(cx - 72f, s * 0.21f, cx + 72f, s * 0.48f), beardBase)

        val bristlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1.4f }
        for (i in 0 until 1300) {
            val angle = rand() * Math.PI.toFloat() * 2f
            val rad = kotlin.math.sqrt(rand().toDouble()).toFloat()
            val bx = cx + cos(angle) * rad * 64f
            val by = s * 0.35f + sin(angle) * rad * 44f
            if (by > s * 0.44f && abs(bx - cx) < 28f) continue // keep cheeks above jawline clean
            bristlePaint.color = if (i % 3 == 0) Color.argb(165, 34, 24, 18) else Color.argb(135, 68, 48, 36)
            canvas.drawLine(bx, by, bx + (rand() - 0.5f) * 2f, by - 4f, bristlePaint)
        }

        // Clean mustache band between nose base & upper lip
        val stachePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(cx, s * 0.45f, cx, s * 0.50f, Color.argb(170, 42, 30, 23), Color.argb(210, 42, 30, 23), Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(RectF(cx - 24f, s * 0.455f, cx + 24f, s * 0.495f), 8f, 8f, stachePaint)

        // Sculpted lips with subtle lip crease & realistic saturation
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        lipPaint.color = Color.rgb(176, 108, 98)
        canvas.drawOval(RectF(cx - 17f, s * 0.422f, cx + 17f, s * 0.444f), lipPaint) // lower lip
        lipPaint.color = Color.rgb(154, 88, 80)
        canvas.drawOval(RectF(cx - 18f, s * 0.442f, cx + 18f, s * 0.458f), lipPaint) // upper lip
        lipPaint.color = Color.argb(160, 92, 46, 42)
        lipPaint.strokeWidth = 1.6f
        canvas.drawLine(cx - 17f, s * 0.443f, cx + 17f, s * 0.443f, lipPaint)

        // Expressive Realistic Eyes (soft orbital shade, almond sclera, deep iris, pupil, upper lash & catchlight)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * 24.5f
            val ey = s * 0.665f

            // Orbital under-eye & upper-lid crease shadow
            p.shader = RadialGradient(ex, ey, 20f, Color.argb(65, 132, 82, 62), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawOval(RectF(ex - 19f, ey - 12f, ex + 19f, ey + 13f), p)
            p.shader = null

            // Almond sclera
            p.color = Color.rgb(236, 234, 230)
            canvas.drawOval(RectF(ex - 11.5f, ey - 6.2f, ex + 11.5f, ey + 6.2f), p)

            // Crisp hazel/steel-blue iris + dark limbal ring
            p.color = Color.rgb(34, 54, 68)
            canvas.drawCircle(ex, ey, 5.4f, p)
            p.color = Color.rgb(58, 102, 128)
            canvas.drawCircle(ex, ey, 4.4f, p)
            p.color = Color.rgb(14, 16, 22)
            canvas.drawCircle(ex, ey, 2.3f, p)
            // Subtle corneal specular catchlight
            p.color = Color.argb(235, 255, 255, 255)
            canvas.drawCircle(ex + 1.5f, ey + 1.4f, 1.0f, p)

            // Upper lash line & eyelid crease
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.4f
            p.color = Color.rgb(36, 26, 22)
            canvas.drawArc(RectF(ex - 12f, ey - 5f, ex + 12f, ey + 7f), 12f, 156f, false, p)
            p.strokeWidth = 1.2f
            p.color = Color.argb(120, 120, 74, 56)
            canvas.drawArc(RectF(ex - 12f, ey - 2f, ex + 12f, ey + 11f), 15f, 150f, false, p)
            p.style = Paint.Style.FILL

            // Natural textured arched eyebrow
            p.color = Color.rgb(34, 25, 20)
            p.strokeWidth = 1.7f
            for (b in 0..22) {
                val frac = b / 22f
                val bx = ex - side * 12f + side * frac * 25f
                val arch = kotlin.math.sin(frac * Math.PI).toFloat() * 3.5f
                val by = ey + 14.5f + arch
                canvas.drawLine(bx, by - 2f, bx + side * 2f, by + 2.5f, p)
            }
        }

        // Clean hairline across back/sides & top crown (`u > 0.44` or `u < 0.06`, plus top v > 0.80)
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        hairPaint.color = Color.rgb(30, 23, 18)
        canvas.drawRect(0f, s * 0.25f, s * 0.095f, s.toFloat(), hairPaint)
        canvas.drawRect(s * 0.405f, s * 0.25f, s.toFloat(), s.toFloat(), hairPaint)
        canvas.drawRect(0f, s * 0.80f, s.toFloat(), s.toFloat(), hairPaint)
        // Sideburns tapering neatly down to jaw
        canvas.drawRect(s * 0.06f, s * 0.42f, s * 0.115f, s * 0.68f, hairPaint)
        canvas.drawRect(s * 0.385f, s * 0.42f, s * 0.44f, s * 0.68f, hairPaint)
        // Subtle hair strands
        hairPaint.strokeWidth = 1.6f
        for (i in 0 until 450) {
            val hx = if (i % 2 == 0) s * (0.42f + rand() * 0.56f) else rand() * s * 0.09f
            val hy = s * (0.28f + rand() * 0.68f)
            hairPaint.color = if (i % 3 == 0) Color.argb(120, 52, 40, 32) else Color.argb(140, 20, 15, 12)
            canvas.drawLine(hx, hy, hx + (rand() - 0.5f) * 4f, hy + 14f, hairPaint)
        }

        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    /**
     * Subtle woven canvas, selvedge denim twill & leather grain micro-texture multiplied onto clothing.
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
                val twill = sin((x + y) * 0.72f) * 0.035f
                val weave = cos((x - y) * 0.72f) * 0.022f
                val fiber = (rand() - 0.5f) * 0.045f
                val v = ((0.94f + twill + weave + fiber) * 255f).toInt().coerceIn(200, 255)
                pixels[y * size + x] = Color.argb(255, v, v, v)
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return uploadTexture(bmp, repeat = true, mipmap = true)
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
