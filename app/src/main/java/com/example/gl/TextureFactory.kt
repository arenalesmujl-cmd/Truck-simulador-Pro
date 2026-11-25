package com.example.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.opengl.GLES30
import android.opengl.GLUtils
import com.example.R
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
    val gaugeFaceTex: Int,
    val skyPhotoTex: Int
) {
    fun release() {
        GLES30.glDeleteTextures(
            11,
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
                gaugeFaceTex,
                skyPhotoTex
            ),
            0
        )
    }
}

object TextureFactory {

    fun createAll(context: Context? = null): WorldTextureSet = WorldTextureSet(
        whiteTex = buildWhiteTexture(),
        asphaltTex = buildAsphaltTexture(),
        terrainTex = buildTerrainTexture(),
        buildingTex = buildBuildingFacadeTexture(),
        corrugatedTex = buildCorrugatedMetalTexture(),
        softShadowTex = buildSoftShadowTexture(),
        glowDiscTex = buildGlowDiscTexture(),
        driverFaceTex = buildDriverFaceTexture(),
        driverClothTex = buildDriverClothTexture(),
        gaugeFaceTex = buildGaugeFaceTexture(),
        skyPhotoTex = buildSkyPhotoTexture(context)
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
                val wear = (1f - (minOf(distTrack1, distTrack2) * 7.2f).coerceIn(0f, 1f)) * 0.14f
                val tarSeam = if (y % 256 in 0..2 || (abs(x - size / 2) <= 1 && (y % 48) < 22)) -0.13f else 0f
                val crack = if ((x + y * 3) % 197 == 0 && rand() > 0.56f) -0.085f else 0f
                val grit = (rand() - 0.5f) * 0.115f + (if (rand() > 0.90f) 0.065f else if (rand() < 0.08f) -0.055f else 0f)
                val macro = sin(x * 0.045f) * cos(y * 0.038f) * 0.032f + sin(x * 0.11f - y * 0.07f) * 0.016f
                val base = (0.62f - wear + grit + macro + crack + tarSeam).coerceIn(0.24f, 0.92f)
                val c = (base * 255f).toInt()
                pixels[y * size + x] = Color.argb(255, c, (c + 1).coerceAtMost(255), (c + 7).coerceAtMost(255))
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
                    Color.rgb(214, 160, 128), // warm sunlit neck base matching torso collar
                    Color.rgb(220, 166, 134), // jaw & chin
                    Color.rgb(236, 185, 151), // mid-face / cheekbones
                    Color.rgb(224, 172, 139)  // forehead
                ),
                floatArrayOf(0.0f, 0.25f, 0.56f, 0.88f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), skinBg)

        // Subtle anatomical under-jaw contour strictly near chin (v=0.22..0.28) so neck join stays clean
        val jawShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                s * 0.25f, s * 0.248f, 64f * k,
                Color.argb(48, 118, 70, 50), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(s * 0.25f - 62f * k, s * 0.205f, s * 0.25f + 62f * k, s * 0.295f), jawShadow)

        // Soft brow-shadow gradient across upper forehead under cap visor
        val visorShadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, s * 0.74f, 0f, s * 0.61f,
                Color.argb(72, 92, 54, 38), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(s * 0.08f, s * 0.61f, s * 0.42f, s * 0.74f, visorShadow)

        // Subtle trapezius & sternocleidomastoid neck warmth along front/sides without hard edges
        val neckShade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                s * 0.25f, s * 0.11f, 70f * k,
                Color.argb(18, 255, 224, 198), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(s * 0.25f - 58f * k, s * 0.02f, s * 0.25f + 58f * k, s * 0.20f), neckShade)

        // Fine organic skin pores, subtle sun warmth & micro-tonal variation
        val porePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        var seed = 31337
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (i in 0 until 4500) {
            val px = rand() * s
            val py = (0.14f + rand() * 0.68f) * s
            porePaint.color = if (i % 2 == 0) Color.argb(9, 152, 92, 66) else Color.argb(8, 252, 214, 188)
            drawTrueCircle(px, py, (0.75f + rand() * 0.95f) * k, porePaint)
        }

        // Front face center X = 0.25 * 1024 = 256f
        val cx = s * 0.25f

        // Warm cheekbone highlight, subtle buccal hollow shade & nasolabial crease
        val cheekPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (side in listOf(-1f, 1f)) {
            cheekPaint.shader = RadialGradient(
                cx + side * 34f * k, s * 0.532f, 24f * k,
                Color.argb(40, 212, 108, 82), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawOval(
                RectF(
                    cx + side * 34f * k - 24f * k, s * 0.532f - 24f * k * 1.35f,
                    cx + side * 34f * k + 24f * k, s * 0.532f + 24f * k * 1.35f
                ),
                cheekPaint
            )
            // Subtle chiseled buccal cheek hollow beneath zygomatic arch
            cheekPaint.shader = RadialGradient(
                cx + side * 37f * k, s * 0.445f, 18f * k,
                Color.argb(38, 122, 74, 56), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            canvas.drawOval(
                RectF(
                    cx + side * 37f * k - 18f * k, s * 0.445f - 18f * k * 1.4f,
                    cx + side * 37f * k + 18f * k, s * 0.445f + 18f * k * 1.4f
                ),
                cheekPaint
            )
            cheekPaint.shader = null
            cheekPaint.style = Paint.Style.STROKE
            cheekPaint.strokeWidth = 1.9f * k
            cheekPaint.color = Color.argb(28, 122, 70, 52)
            canvas.drawArc(
                RectF(cx + side * 12f * k - 12f * k, s * 0.410f, cx + side * 12f * k + 18f * k, s * 0.515f),
                if (side > 0f) 115f else 25f, 45f, false, cheekPaint
            )
            cheekPaint.style = Paint.Style.FILL
        }

        // Sculpted nose dorsal highlight, defined alar creases & anatomical nostrils
        val nosePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        nosePaint.shader = LinearGradient(
            cx, s * 0.495f, cx, s * 0.614f,
            Color.argb(62, 255, 240, 224), Color.argb(14, 255, 240, 224), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(RectF(cx - 3.6f * k, s * 0.496f, cx + 3.6f * k, s * 0.612f), 3.6f * k, 9f * k, nosePaint)
        nosePaint.shader = RadialGradient(cx, s * 0.492f, 15f * k, Color.argb(70, 126, 74, 54), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawOval(RectF(cx - 13f * k, s * 0.474f, cx + 13f * k, s * 0.514f), nosePaint)
        nosePaint.shader = null
        nosePaint.color = Color.argb(165, 68, 34, 26)
        canvas.drawOval(RectF(cx - 7.8f * k, s * 0.483f, cx - 2.8f * k, s * 0.497f), nosePaint)
        canvas.drawOval(RectF(cx + 2.8f * k, s * 0.483f, cx + 7.8f * k, s * 0.497f), nosePaint)

        // Subtle philtrum groove
        nosePaint.color = Color.argb(34, 138, 82, 62)
        canvas.drawRoundRect(RectF(cx - 2.8f * k, s * 0.448f, cx + 2.8f * k, s * 0.482f), 2.8f * k, 6f * k, nosePaint)

        // Tailored short-boxed beard with ultra-fine feathered cheek & jawline transition (clean neck!)
        val beardBase = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx, s * 0.346f, 60f * k,
                intArrayOf(
                    Color.argb(104, 42, 29, 22),
                    Color.argb(58, 50, 35, 27),
                    Color.argb(0, 50, 35, 27)
                ),
                floatArrayOf(0f, 0.50f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawOval(RectF(cx - 52f * k, s * 0.258f, cx + 52f * k, s * 0.446f), beardBase)

        val bristlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 0.85f * k
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 4200) {
            val angle = rand() * Math.PI.toFloat() * 2f
            val rad = sqrt(rand().toDouble()).toFloat()
            val bx = cx + cos(angle) * rad * 57f * k
            val dxNorm = ((bx - cx) / (57f * k)).coerceIn(-1f, 1f)
            val jawEdgeY = s * (0.260f + 0.084f * dxNorm * dxNorm)
            val by = s * 0.350f + sin(angle) * rad * 54f * k
            val edgeFade = ((by - jawEdgeY) / (s * 0.032f)).coerceIn(0f, 1f)
            if (edgeFade <= 0.05f || rand() > edgeFade * 1.15f) continue
            if (by > s * 0.392f && by < s * 0.444f && abs(bx - cx) < 22f * k) continue
            if (by > s * 0.445f && abs(bx - cx) > 18f * k) continue
            val alphaScale = (0.42f + 0.58f * edgeFade) * (1f - 0.42f * rad)
            bristlePaint.color = when (i % 4) {
                0 -> Color.argb((132 * alphaScale).toInt(), 24, 17, 13)
                1 -> Color.argb((105 * alphaScale).toInt(), 64, 45, 34)
                else -> Color.argb((118 * alphaScale).toInt(), 36, 25, 19)
            }
            val hairLen = (2.8f + rand() * 2.0f) * k
            canvas.drawLine(bx, by, bx + (rand() - 0.5f) * 1.3f * k, by - hairLen, bristlePaint)
        }

        // Contoured natural mustache hairs with clean Cupid's bow arch
        val stacheSoft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(cx, s * 0.453f, 19f * k, Color.argb(96, 34, 24, 18), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        }
        canvas.drawOval(RectF(cx - 20f * k, s * 0.437f, cx + 20f * k, s * 0.471f), stacheSoft)
        for (i in 0 until 290) {
            val mx = cx + (rand() - 0.5f) * 34f * k
            val my = s * (0.442f + rand() * 0.025f)
            bristlePaint.color = Color.argb(126, 28, 19, 14)
            canvas.drawLine(mx, my, mx + (mx - cx) * 0.08f, my - 3.4f * k, bristlePaint)
        }

        // Sculpted natural masculine lips with feathered Cupid's-bow, volumetric lower-lip highlight & soft corners
        val lipSoft = Paint(Paint.ANTI_ALIAS_FLAG)
        lipSoft.shader = RadialGradient(
            cx, s * 0.422f, 16.5f * k,
            intArrayOf(Color.argb(115, 162, 106, 94), Color.argb(46, 178, 124, 110), Color.TRANSPARENT),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(cx - 17f * k, s * 0.398f, cx + 17f * k, s * 0.445f), lipSoft)
        lipSoft.shader = null

        val lowerLipPath = android.graphics.Path().apply {
            moveTo(cx - 13.5f * k, s * 0.421f)
            cubicTo(cx - 7.5f * k, s * 0.402f, cx + 7.5f * k, s * 0.402f, cx + 13.5f * k, s * 0.421f)
            quadTo(cx, s * 0.415f, cx - 13.5f * k, s * 0.421f)
            close()
        }
        lipSoft.color = Color.argb(182, 164, 108, 96)
        canvas.drawPath(lowerLipPath, lipSoft)

        val upperLipPath = android.graphics.Path().apply {
            moveTo(cx - 14.5f * k, s * 0.421f)
            cubicTo(cx - 8f * k, s * 0.436f, cx - 3.5f * k, s * 0.439f, cx, s * 0.433f)
            cubicTo(cx + 3.5f * k, s * 0.439f, cx + 8f * k, s * 0.436f, cx + 14.5f * k, s * 0.421f)
            quadTo(cx, s * 0.419f, cx - 14.5f * k, s * 0.421f)
            close()
        }
        lipSoft.color = Color.argb(192, 138, 86, 76)
        canvas.drawPath(upperLipPath, lipSoft)

        lipSoft.shader = RadialGradient(
            cx, s * 0.411f, 8.5f * k,
            Color.argb(54, 248, 214, 198), Color.TRANSPARENT, Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(cx - 8.5f * k, s * 0.404f, cx + 8.5f * k, s * 0.418f), lipSoft)
        lipSoft.shader = null

        lipSoft.style = Paint.Style.STROKE
        lipSoft.strokeCap = Paint.Cap.ROUND
        lipSoft.strokeWidth = 1.15f * k
        lipSoft.color = Color.argb(145, 76, 40, 34)
        val lipPartPath = android.graphics.Path().apply {
            moveTo(cx - 14.2f * k, s * 0.422f)
            quadTo(cx - 5f * k, s * 0.420f, cx, s * 0.421f)
            quadTo(cx + 5f * k, s * 0.420f, cx + 14.2f * k, s * 0.422f)
        }
        canvas.drawPath(lipPartPath, lipSoft)
        lipSoft.style = Paint.Style.FILL
        for (side in listOf(-1f, 1f)) {
            lipSoft.shader = RadialGradient(
                cx + side * 14.5f * k, s * 0.421f, 4.2f * k,
                Color.argb(74, 88, 48, 38), Color.TRANSPARENT, Shader.TileMode.CLAMP
            )
            drawTrueCircle(cx + side * 14.5f * k, s * 0.421f, 4.2f * k, lipSoft)
            lipSoft.shader = null
        }

        // Photorealistic 1024px Almond-Clipped Eyes with eyelid occlusion shadow, golden-hazel iris & natural layered brows
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * 26.8f * k
            val ey = s * 0.600f
            val eyeRx = 10.6f * k
            val eyeRy = 4.9f * k * yScale

            // Deep orbital socket AO shade & subtle tear-trough warmth
            p.shader = RadialGradient(ex, ey, 22f * k, Color.argb(88, 102, 58, 42), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawOval(RectF(ex - 20f * k, ey - 20f * k * yScale * 0.65f, ex + 20f * k, ey + 20f * k * yScale * 0.65f), p)
            p.shader = null

            val almondPath = android.graphics.Path().apply {
                moveTo(ex - eyeRx, ey - 0.3f * k * yScale)
                quadTo(ex, ey + eyeRy * 1.15f, ex + eyeRx, ey - 0.3f * k * yScale)
                quadTo(ex, ey - eyeRy * 1.05f, ex - eyeRx, ey - 0.3f * k * yScale)
                close()
            }

            canvas.save()
            canvas.clipPath(almondPath)

            p.color = Color.rgb(208, 203, 198)
            canvas.drawRect(ex - eyeRx - 2f, ey - eyeRy - 2f, ex + eyeRx + 2f, ey + eyeRy + 2f, p)
            p.color = Color.argb(112, 184, 104, 92)
            drawTrueCircle(ex - side * 8.6f * k, ey, 1.85f * k, p)

            // Multi-layered steel-blue & warm amber-hazel limbal iris + radial stroma + crisp pupil
            p.color = Color.rgb(16, 26, 38)
            drawTrueCircle(ex, ey, 4.75f * k, p)
            p.color = Color.rgb(44, 86, 114)
            drawTrueCircle(ex, ey, 4.05f * k, p)
            p.color = Color.rgb(92, 140, 162)
            drawTrueCircle(ex, ey - 0.4f * k * yScale, 2.80f * k, p)
            p.color = Color.argb(118, 162, 134, 82)
            drawTrueCircle(ex, ey, 2.35f * k, p)
            p.strokeWidth = 0.85f * k
            for (fi in 0 until 28) {
                val fa = (fi / 28f) * Math.PI.toFloat() * 2f
                p.color = when (fi % 3) {
                    0 -> Color.argb(122, 152, 198, 220)
                    1 -> Color.argb(105, 194, 158, 98)
                    else -> Color.argb(95, 28, 56, 78)
                }
                canvas.drawLine(
                    ex + cos(fa) * 1.85f * k, ey + sin(fa) * 1.85f * k * yScale,
                    ex + cos(fa) * 3.95f * k, ey + sin(fa) * 3.95f * k * yScale,
                    p
                )
            }
            p.color = Color.rgb(6, 8, 12)
            drawTrueCircle(ex, ey, 1.88f * k, p)

            p.shader = LinearGradient(
                ex, ey + eyeRy * 0.05f, ex, ey + eyeRy * 0.95f,
                Color.argb(0, 34, 22, 18), Color.argb(138, 24, 15, 12), Shader.TileMode.CLAMP
            )
            canvas.drawRect(ex - eyeRx, ey, ex + eyeRx, ey + eyeRy, p)
            p.shader = null

            p.color = Color.argb(236, 248, 250, 255)
            drawTrueCircle(ex + 1.35f * k, ey + 1.05f * k * yScale, 0.84f * k, p)
            p.color = Color.argb(132, 255, 255, 255)
            drawTrueCircle(ex - 1.15f * k, ey - 1.10f * k * yScale, 0.42f * k, p)

            canvas.restore()

            p.style = Paint.Style.STROKE
            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 2.9f * k
            p.color = Color.rgb(22, 15, 11)
            val upperLash = android.graphics.Path().apply {
                moveTo(ex - eyeRx, ey - 0.2f * k * yScale)
                quadTo(ex, ey + eyeRy * 1.15f, ex + eyeRx, ey - 0.2f * k * yScale)
            }
            canvas.drawPath(upperLash, p)

            p.strokeWidth = 1.35f * k
            p.color = Color.argb(150, 96, 58, 40)
            val lidCrease = android.graphics.Path().apply {
                moveTo(ex - eyeRx * 0.96f, ey + 1.4f * k * yScale)
                quadTo(ex, ey + eyeRy * 1.72f, ex + eyeRx * 0.96f, ey + 1.4f * k * yScale)
            }
            canvas.drawPath(lidCrease, p)

            p.strokeWidth = 1.05f * k
            p.color = Color.argb(82, 86, 52, 36)
            val lowerLid = android.graphics.Path().apply {
                moveTo(ex - eyeRx * 0.90f, ey - 0.6f * k * yScale)
                quadTo(ex, ey - eyeRy * 1.05f, ex + eyeRx * 0.90f, ey - 0.6f * k * yScale)
            }
            canvas.drawPath(lowerLid, p)
            p.style = Paint.Style.FILL

            // Soft feathered eyebrow tint base + ultra-fine multi-layered curved brow hairs
            p.shader = LinearGradient(
                ex - side * 13.5f * k, s * 0.654f, ex + side * 14.5f * k, s * 0.654f,
                intArrayOf(Color.argb(62, 32, 22, 16), Color.argb(122, 28, 19, 14), Color.argb(38, 32, 22, 16)),
                floatArrayOf(0f, 0.45f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawOval(RectF(ex - 14f * k, s * 0.646f, ex + 14f * k, s * 0.665f), p)
            p.shader = null

            p.strokeCap = Paint.Cap.ROUND
            p.strokeWidth = 0.78f * k
            for (b in 0..138) {
                val frac = b / 138f
                val bx = ex - side * 13.4f * k + side * frac * 27.6f * k
                val arch = sin(frac * Math.PI).toFloat() * 5.2f * k
                val taper = 0.55f + 0.48f * sin(frac * Math.PI).toFloat()
                val jitter = ((b * 29) % 7 - 3) * 0.45f * k
                val by = s * 0.654f + arch + jitter
                p.color = when (b % 3) {
                    0 -> Color.argb(165, 52, 37, 27)
                    1 -> Color.argb(195, 24, 17, 13)
                    else -> Color.argb(180, 34, 24, 18)
                }
                canvas.drawLine(bx, by - 3.2f * k * taper, bx + side * 2.6f * k, by + 2.8f * k * taper, p)
            }
        }

        // Contoured natural hairline & feathered nape/sideburns + subtle front bangs under cap
        val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        hairPaint.color = Color.rgb(24, 18, 14)
        canvas.drawRect(0f, s * 0.742f, s.toFloat(), s.toFloat(), hairPaint)

        // Feathered front hairline fringe at temples & brow peak right under the cap rim
        hairPaint.strokeWidth = 1.9f * k
        hairPaint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 210) {
            val frac = i / 210f
            val bx = cx + (frac - 0.5f) * 145f * k
            val archDrop = (1f - abs(frac - 0.5f) * 1.6f).coerceAtLeast(0f) * 0.015f
            val byTop = s * 0.752f
            val byBot = s * (0.722f - archDrop - (rand() * 0.014f))
            hairPaint.color = if (i % 3 == 0) Color.argb(175, 46, 34, 26) else Color.argb(205, 22, 16, 12)
            canvas.drawLine(bx, byTop, bx + (bx - cx) * 0.05f, byBot, hairPaint)
        }
        hairPaint.strokeWidth = 1f

        // Smooth curved occipital hairline from above ears (v=0.58) down to tapered nape (v=0.38)
        val backHairPath = android.graphics.Path().apply {
            moveTo(s * 0.445f, s * 0.750f)
            lineTo(s * 0.445f, s * 0.460f)
            cubicTo(s * 0.490f, s * 0.435f, s * 0.560f, s * 0.388f, s * 0.680f, s * 0.385f)
            lineTo(s * 0.820f, s * 0.385f)
            cubicTo(s * 0.935f, s * 0.388f, s * 0.975f, s * 0.435f, s.toFloat(), s * 0.455f)
            lineTo(s.toFloat(), s * 0.750f)
            close()
        }
        canvas.drawPath(backHairPath, hairPaint)

        // Wrap-around right-side temple/ear-back portion (u = 0.0 .. 0.055)
        val rightTemplePath = android.graphics.Path().apply {
            moveTo(0f, s * 0.750f)
            lineTo(0f, s * 0.455f)
            quadTo(s * 0.032f, s * 0.465f, s * 0.056f, s * 0.495f)
            lineTo(s * 0.058f, s * 0.750f)
            close()
        }
        canvas.drawPath(rightTemplePath, hairPaint)

        // Soft feathered horizontal & nape fade so zero rectangular edges ever show
        hairPaint.shader = LinearGradient(s * 0.050f, 0f, s * 0.105f, 0f, Color.argb(215, 24, 18, 14), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(s * 0.050f, s * 0.47f, s * 0.105f, s * 0.75f, hairPaint)
        hairPaint.shader = LinearGradient(s * 0.450f, 0f, s * 0.395f, 0f, Color.argb(215, 24, 18, 14), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(s * 0.395f, s * 0.47f, s * 0.450f, s * 0.75f, hairPaint)
        hairPaint.shader = LinearGradient(0f, s * 0.395f, 0f, s * 0.352f, Color.argb(210, 24, 18, 14), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(s * 0.46f, s * 0.352f, s.toFloat(), s * 0.395f, hairPaint)
        hairPaint.shader = null

        // Tapered hair lock strokes strictly above the nape/temple curves
        hairPaint.strokeWidth = 1.4f * k
        hairPaint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 920) {
            val hx = if (i % 2 == 0) s * (0.43f + rand() * 0.56f) else rand() * s * 0.082f
            val u = hx / s
            val minV = if (u > 0.43f) {
                val edge = abs(u - 0.72f) / 0.29f
                0.368f + edge * edge * 0.085f
            } else {
                0.455f + (u / 0.085f) * 0.055f
            }
            val hy = s * (minV + rand() * (0.94f - minV))
            hairPaint.color = if (i % 3 == 0) Color.argb(122, 54, 42, 34) else Color.argb(145, 18, 13, 10)
            canvas.drawLine(hx, hy, hx + (rand() - 0.5f) * 3.2f * k, hy - 14f * k, hairPaint)
        }

        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    /**
     * Ultra-fine woven canvas, selvedge denim twill & leather micro-texture multiplied smoothly onto apparel.
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
                val twill = sin((x + y) * 1.15f) * 0.032f
                val weave = cos((x - y) * 1.15f) * 0.022f
                val fold = sin(y * 0.065f + sin(x * 0.030f) * 1.1f) * 0.019f
                val stitch = if (y % 64 in 0..1 && (x % 12) < 7) 0.026f else 0f
                val fiber = (rand() - 0.5f) * 0.024f
                val v = ((0.958f + twill + weave + fold + fiber + stitch) * 255f).toInt().coerceIn(220, 255)
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

        // Crisp illuminated cyan accent arc, inner telemetry band & machined hub bezel
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5.5f
            color = Color.argb(195, 56, 189, 248)
        }
        canvas.drawArc(RectF(size * 0.12f, size * 0.12f, size * 0.88f, size * 0.88f), 135f, 225f, false, ringPaint)
        ringPaint.color = Color.argb(225, 244, 63, 94)
        canvas.drawArc(RectF(size * 0.12f, size * 0.12f, size * 0.88f, size * 0.88f), 360f, 45f, false, ringPaint)
        ringPaint.strokeWidth = 2.0f
        ringPaint.color = Color.argb(95, 251, 146, 60)
        canvas.drawArc(RectF(size * 0.25f, size * 0.25f, size * 0.75f, size * 0.75f), 135f, 270f, false, ringPaint)
        ringPaint.strokeWidth = 2.5f
        ringPaint.color = Color.argb(110, 148, 163, 184)
        canvas.drawCircle(cx, cy, size * 0.21f, ringPaint)

        return uploadTexture(bmp, repeat = false, mipmap = true)
    }

    private fun buildSkyPhotoTexture(context: Context?): Int {
        val rawBmp = if (context != null) {
            try {
                val opts = BitmapFactory.Options().apply {
                    inScaled = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeResource(context.resources, R.drawable.sky_realistic, opts)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

        if (rawBmp == null) {
            val fallback = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(fallback)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, 256f,
                    Color.rgb(8, 72, 178),
                    Color.rgb(112, 184, 246),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, 256f, 256f, paint)
            return uploadTexture(fallback, repeat = false, mipmap = true, repeatVertical = false)
        }

        return uploadTexture(rawBmp, repeat = false, mipmap = true, repeatVertical = false)
    }

    private fun uploadTexture(
        bmp: Bitmap,
        repeat: Boolean,
        mipmap: Boolean,
        repeatVertical: Boolean = repeat
    ): Int {
        val ids = IntArray(1)
        GLES30.glGenTextures(1, ids, 0)
        val texId = ids[0]
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId)
        val wrapS = if (repeat) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
        val wrapT = if (repeatVertical) GLES30.GL_REPEAT else GLES30.GL_CLAMP_TO_EDGE
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, wrapS)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, wrapT)
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
