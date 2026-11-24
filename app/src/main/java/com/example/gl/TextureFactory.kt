package com.example.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
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
    val glowDiscTex: Int
) {
    fun release() {
        GLES30.glDeleteTextures(
            7,
            intArrayOf(whiteTex, asphaltTex, terrainTex, buildingTex, corrugatedTex, softShadowTex, glowDiscTex),
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
        glowDiscTex = buildGlowDiscTexture()
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
                // Subtle worn tire tracks in left & right lanes
                val distTrack1 = abs(u - 0.27f)
                val distTrack2 = abs(u - 0.73f)
                val wear = (1f - (minOf(distTrack1, distTrack2) * 8f).coerceIn(0f, 1f)) * 0.08f

                // Fine mineral aggregate speckle
                val grain = (rand() - 0.5f) * 0.14f
                val macro = sin(x * 0.05f) * cos(y * 0.04f) * 0.025f
                val base = (0.76f - wear + grain + macro).coerceIn(0.42f, 0.98f)
                val c = (base * 255f).toInt()
                pixels[y * size + x] = Color.argb(255, c, c, (c + 3).coerceAtMost(255))
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    private fun buildTerrainTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        var seed = 44219
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }
        for (y in 0 until size) {
            for (x in 0 until size) {
                val patch = sin(x * 0.06f) * cos(y * 0.06f) * 0.07f
                val blade = (rand() - 0.5f) * 0.16f
                val r = ((0.84f + patch + blade) * 255f).toInt().coerceIn(140, 255)
                val g = ((0.92f + patch + blade) * 255f).toInt().coerceIn(160, 255)
                val b = ((0.80f + patch + blade * 0.8f) * 255f).toInt().coerceIn(130, 255)
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
        // Concrete structural background
        canvas.drawColor(Color.rgb(185, 192, 204))

        val cols = 8
        val rows = 12
        val cellW = size.toFloat() / cols
        val cellH = size.toFloat() / rows
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val mullionPaint = Paint().apply {
            color = Color.rgb(70, 78, 92)
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        var seed = 91827
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 1000) / 1000f
        }

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x0 = c * cellW + 5f
                val y0 = r * cellH + 5f
                val x1 = (c + 1) * cellW - 5f
                val y1 = (r + 1) * cellH - 5f
                val tint = rand()
                val topCol = if (tint > 0.72f) Color.rgb(145, 185, 220) else Color.rgb(88, 122, 155)
                val botCol = Color.rgb(42, 62, 88)
                glassPaint.shader = LinearGradient(x0, y0, x0, y1, topCol, botCol, Shader.TileMode.CLAMP)
                canvas.drawRect(x0, y0, x1, y1, glassPaint)
                canvas.drawRect(x0, y0, x1, y1, mullionPaint)
            }
        }
        return uploadTexture(bmp, repeat = true, mipmap = true)
    }

    private fun buildCorrugatedMetalTexture(): Int {
        val size = 256
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val rib = sin((x.toFloat() / size) * 2f * Math.PI.toFloat() * 14f) * 0.08f
                val seam = if (y % 64 < 2 || x % 128 < 2) -0.10f else 0f
                val v = ((0.90f + rib + seam) * 255f).toInt().coerceIn(160, 255)
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
