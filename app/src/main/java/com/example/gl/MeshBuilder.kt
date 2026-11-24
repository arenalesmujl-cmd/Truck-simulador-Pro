package com.example.gl

import android.opengl.GLES30
import com.example.sim.TruckWorld
import com.example.sim.ZoneType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt

data class LoftRingY(
    val y: Float,
    val centerZ: Float,
    val rx: Float,
    val rz: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val roundness: Float = 1.0f // 1.0 = smooth ellipse, < 1.0 = athletic/squared superellipse
)

data class LoftRingZ(
    val z: Float,
    val centerY: Float,
    val rx: Float,
    val ry: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val roundness: Float = 0.42f // automotive rounded-corner sheet metal
)

data class WheelMeshSet(
    val tireMesh: MeshBuffer,
    val rimMesh: MeshBuffer
)

class MeshBuffer(
    vertices: FloatArray,
    private val drawMode: Int = GLES30.GL_TRIANGLES
) {
    // Stride: pos(3) + normal(3) + uv(2) + color(4) = 12 floats = 48 bytes
    val vertexCount: Int = vertices.size / 12
    private val vaoId: Int
    private val vboId: Int

    init {
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        vaoId = vaos[0]
        vboId = vbos[0]

        val fb: FloatBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        fb.position(0)

        GLES30.glBindVertexArray(vaoId)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, fb, GLES30.GL_STATIC_DRAW)

        val strideBytes = 12 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)
        GLES30.glEnableVertexAttribArray(3)
        GLES30.glVertexAttribPointer(3, 4, GLES30.GL_FLOAT, false, strideBytes, 8 * 4)

        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (vertexCount <= 0) return
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(drawMode, 0, vertexCount)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
    }
}

class DynamicMeshBuilder {
    private val data = ArrayList<Float>(16384)

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(u); data.add(v)
        data.add(r); data.add(g); data.add(b); data.add(a)
    }

    fun addQuadUv(
        x1: Float, y1: Float, z1: Float, u1: Float, v1: Float,
        x2: Float, y2: Float, z2: Float, u2: Float, v2: Float,
        x3: Float, y3: Float, z3: Float, u3: Float, v3: Float,
        x4: Float, y4: Float, z4: Float, u4: Float, v4: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addVertex(x1, y1, z1, nx, ny, nz, u1, v1, r, g, b, a)
        addVertex(x2, y2, z2, nx, ny, nz, u2, v2, r, g, b, a)
        addVertex(x3, y3, z3, nx, ny, nz, u3, v3, r, g, b, a)

        addVertex(x1, y1, z1, nx, ny, nz, u1, v1, r, g, b, a)
        addVertex(x3, y3, z3, nx, ny, nz, u3, v3, r, g, b, a)
        addVertex(x4, y4, z4, nx, ny, nz, u4, v4, r, g, b, a)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addQuadUv(
            x1, y1, z1, 0f, 0f,
            x2, y2, z2, 1f, 0f,
            x3, y3, z3, 1f, 1f,
            x4, y4, z4, 0f, 1f,
            nx, ny, nz, r, g, b, a
        )
    }

    private fun superPow(v: Float, p: Float): Float = sign(v) * abs(v).pow(p)

    /**
     * Builds an organic smooth-shaded anatomical mesh along Y from ordered cross-section rings.
     */
    fun addLoftedProfileY(
        cx: Float,
        rings: List<LoftRingY>,
        sectors: Int = 18,
        cappedBottom: Boolean = true,
        cappedTop: Boolean = true
    ) {
        if (rings.size < 2) return
        val nRings = rings.size
        val posX = Array(nRings) { FloatArray(sectors) }
        val posY = Array(nRings) { FloatArray(sectors) }
        val posZ = Array(nRings) { FloatArray(sectors) }
        val norX = Array(nRings) { FloatArray(sectors) }
        val norY = Array(nRings) { FloatArray(sectors) }
        val norZ = Array(nRings) { FloatArray(sectors) }

        for (ri in 0 until nRings) {
            val ring = rings[ri]
            for (s in 0 until sectors) {
                val a = (s.toFloat() / sectors) * 2f * PI.toFloat()
                val ca = superPow(cos(a), ring.roundness)
                val sa = superPow(sin(a), ring.roundness)
                posX[ri][s] = cx + ca * ring.rx
                posY[ri][s] = ring.y
                posZ[ri][s] = ring.centerZ + sa * ring.rz
            }
        }

        // Smooth averaged vertex normals across neighboring rings & radial sectors
        for (ri in 0 until nRings) {
            val prevR = (ri - 1).coerceAtLeast(0)
            val nextR = (ri + 1).coerceAtMost(nRings - 1)
            for (s in 0 until sectors) {
                val prevS = (s - 1 + sectors) % sectors
                val nextS = (s + 1) % sectors
                val tx = posX[ri][nextS] - posX[ri][prevS]
                val ty = posY[ri][nextS] - posY[ri][prevS]
                val tz = posZ[ri][nextS] - posZ[ri][prevS]
                val bx = posX[nextR][s] - posX[prevR][s]
                val by = posY[nextR][s] - posY[prevR][s]
                val bz = posZ[nextR][s] - posZ[prevR][s]
                // Cross tangent x bitangent -> outward normal
                var nx = by * tz - bz * ty
                var ny = bz * tx - bx * tz
                var nz = bx * ty - by * tx
                val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
                nx /= len; ny /= len; nz /= len
                norX[ri][s] = nx; norY[ri][s] = ny; norZ[ri][s] = nz
            }
        }

        for (ri in 0 until nRings - 1) {
            val r0 = rings[ri]
            val r1 = rings[ri + 1]
            val v0 = ri.toFloat() / (nRings - 1)
            val v1 = (ri + 1).toFloat() / (nRings - 1)
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                val u0 = s.toFloat() / sectors
                val u1 = (s + 1).toFloat() / sectors

                addVertex(posX[ri][s], posY[ri][s], posZ[ri][s], norX[ri][s], norY[ri][s], norZ[ri][s], u0, v0, r0.r, r0.g, r0.b)
                addVertex(posX[ri][sNext], posY[ri][sNext], posZ[ri][sNext], norX[ri][sNext], norY[ri][sNext], norZ[ri][sNext], u1, v0, r0.r, r0.g, r0.b)
                addVertex(posX[ri + 1][sNext], posY[ri + 1][sNext], posZ[ri + 1][sNext], norX[ri + 1][sNext], norY[ri + 1][sNext], norZ[ri + 1][sNext], u1, v1, r1.r, r1.g, r1.b)

                addVertex(posX[ri][s], posY[ri][s], posZ[ri][s], norX[ri][s], norY[ri][s], norZ[ri][s], u0, v0, r0.r, r0.g, r0.b)
                addVertex(posX[ri + 1][sNext], posY[ri + 1][sNext], posZ[ri + 1][sNext], norX[ri + 1][sNext], norY[ri + 1][sNext], norZ[ri + 1][sNext], u1, v1, r1.r, r1.g, r1.b)
                addVertex(posX[ri + 1][s], posY[ri + 1][s], posZ[ri + 1][s], norX[ri + 1][s], norY[ri + 1][s], norZ[ri + 1][s], u0, v1, r1.r, r1.g, r1.b)
            }
        }

        if (cappedTop) {
            val top = rings.last()
            val lastIdx = nRings - 1
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                addVertex(cx, top.y, top.centerZ, 0f, 1f, 0f, 0.5f, 0.5f, top.r, top.g, top.b)
                addVertex(posX[lastIdx][s], top.y, posZ[lastIdx][s], 0f, 1f, 0f, 0.5f, 0.5f, top.r, top.g, top.b)
                addVertex(posX[lastIdx][sNext], top.y, posZ[lastIdx][sNext], 0f, 1f, 0f, 0.5f, 0.5f, top.r, top.g, top.b)
            }
        }
        if (cappedBottom) {
            val bot = rings.first()
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                addVertex(cx, bot.y, bot.centerZ, 0f, -1f, 0f, 0.5f, 0.5f, bot.r * 0.85f, bot.g * 0.85f, bot.b * 0.85f)
                addVertex(posX[0][sNext], bot.y, posZ[0][sNext], 0f, -1f, 0f, 0.5f, 0.5f, bot.r * 0.85f, bot.g * 0.85f, bot.b * 0.85f)
                addVertex(posX[0][s], bot.y, posZ[0][s], 0f, -1f, 0f, 0.5f, 0.5f, bot.r * 0.85f, bot.g * 0.85f, bot.b * 0.85f)
            }
        }
    }

    /**
     * Builds rounded-corner aerodynamic automotive sheet-metal shells along Z (cabs, sleeper roofs, sculpted hoods).
     */
    fun addLoftedProfileZ(
        cx: Float,
        rings: List<LoftRingZ>,
        sectors: Int = 24,
        cappedFront: Boolean = true,
        cappedBack: Boolean = true
    ) {
        if (rings.size < 2) return
        val nRings = rings.size
        val posX = Array(nRings) { FloatArray(sectors) }
        val posY = Array(nRings) { FloatArray(sectors) }
        val posZ = Array(nRings) { FloatArray(sectors) }
        val norX = Array(nRings) { FloatArray(sectors) }
        val norY = Array(nRings) { FloatArray(sectors) }
        val norZ = Array(nRings) { FloatArray(sectors) }

        for (ri in 0 until nRings) {
            val ring = rings[ri]
            for (s in 0 until sectors) {
                val a = (s.toFloat() / sectors) * 2f * PI.toFloat()
                val ca = superPow(cos(a), ring.roundness)
                val sa = superPow(sin(a), ring.roundness)
                posX[ri][s] = cx + ca * ring.rx
                posY[ri][s] = ring.centerY + sa * ring.ry
                posZ[ri][s] = ring.z
            }
        }

        for (ri in 0 until nRings) {
            val prevR = (ri - 1).coerceAtLeast(0)
            val nextR = (ri + 1).coerceAtMost(nRings - 1)
            for (s in 0 until sectors) {
                val prevS = (s - 1 + sectors) % sectors
                val nextS = (s + 1) % sectors
                val tx = posX[ri][nextS] - posX[ri][prevS]
                val ty = posY[ri][nextS] - posY[ri][prevS]
                val tz = posZ[ri][nextS] - posZ[ri][prevS]
                val bx = posX[nextR][s] - posX[prevR][s]
                val by = posY[nextR][s] - posY[prevR][s]
                val bz = posZ[nextR][s] - posZ[prevR][s]
                var nx = ty * bz - tz * by
                var ny = tz * bx - tx * bz
                var nz = tx * by - ty * bx
                val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
                nx /= len; ny /= len; nz /= len
                norX[ri][s] = nx; norY[ri][s] = ny; norZ[ri][s] = nz
            }
        }

        for (ri in 0 until nRings - 1) {
            val r0 = rings[ri]
            val r1 = rings[ri + 1]
            val v0 = ri.toFloat() / (nRings - 1)
            val v1 = (ri + 1).toFloat() / (nRings - 1)
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                val u0 = s.toFloat() / sectors
                val u1 = (s + 1).toFloat() / sectors

                addVertex(posX[ri][s], posY[ri][s], posZ[ri][s], norX[ri][s], norY[ri][s], norZ[ri][s], u0, v0, r0.r, r0.g, r0.b)
                addVertex(posX[ri + 1][sNext], posY[ri + 1][sNext], posZ[ri + 1][sNext], norX[ri + 1][sNext], norY[ri + 1][sNext], norZ[ri + 1][sNext], u1, v1, r1.r, r1.g, r1.b)
                addVertex(posX[ri][sNext], posY[ri][sNext], posZ[ri][sNext], norX[ri][sNext], norY[ri][sNext], norZ[ri][sNext], u1, v0, r0.r, r0.g, r0.b)

                addVertex(posX[ri][s], posY[ri][s], posZ[ri][s], norX[ri][s], norY[ri][s], norZ[ri][s], u0, v0, r0.r, r0.g, r0.b)
                addVertex(posX[ri + 1][s], posY[ri + 1][s], posZ[ri + 1][s], norX[ri + 1][s], norY[ri + 1][s], norZ[ri + 1][s], u0, v1, r1.r, r1.g, r1.b)
                addVertex(posX[ri + 1][sNext], posY[ri + 1][sNext], posZ[ri + 1][sNext], norX[ri + 1][sNext], norY[ri + 1][sNext], norZ[ri + 1][sNext], u1, v1, r1.r, r1.g, r1.b)
            }
        }

        if (cappedFront) {
            val front = rings.last()
            val lastIdx = nRings - 1
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                addVertex(cx, front.centerY, front.z, 0f, 0f, 1f, 0.5f, 0.5f, front.r, front.g, front.b)
                addVertex(posX[lastIdx][s], posY[lastIdx][s], front.z, 0f, 0f, 1f, 0.5f, 0.5f, front.r, front.g, front.b)
                addVertex(posX[lastIdx][sNext], posY[lastIdx][sNext], front.z, 0f, 0f, 1f, 0.5f, 0.5f, front.r, front.g, front.b)
            }
        }
        if (cappedBack) {
            val back = rings.first()
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                addVertex(cx, back.centerY, back.z, 0f, 0f, -1f, 0.5f, 0.5f, back.r * 0.9f, back.g * 0.9f, back.b * 0.9f)
                addVertex(posX[0][sNext], posY[0][sNext], back.z, 0f, 0f, -1f, 0.5f, 0.5f, back.r * 0.9f, back.g * 0.9f, back.b * 0.9f)
                addVertex(posX[0][s], posY[0][s], back.z, 0f, 0f, -1f, 0.5f, 0.5f, back.r * 0.9f, back.g * 0.9f, back.b * 0.9f)
            }
        }
    }

    /**
     * Smooth curved baseball/trucker cap visor swept smoothly across the brow.
     */
    fun addCurvedVisor(
        cy: Float, cz: Float,
        width: Float, length: Float, thickness: Float, archHeight: Float,
        segs: Int = 12,
        r: Float, g: Float, b: Float
    ) {
        for (i in 0 until segs) {
            val t0 = (i.toFloat() / segs) * 2f - 1f
            val t1 = ((i + 1).toFloat() / segs) * 2f - 1f
            val x0 = t0 * (width * 0.5f)
            val x1 = t1 * (width * 0.5f)
            val y0 = cy - (t0 * t0) * archHeight
            val y1 = cy - (t1 * t1) * archHeight
            val zBack0 = cz - (t0 * t0) * 0.025f
            val zBack1 = cz - (t1 * t1) * 0.025f
            val tip0 = (1f - t0 * t0 * 0.42f) * length
            val tip1 = (1f - t1 * t1 * 0.42f) * length
            val zFront0 = zBack0 + tip0
            val zFront1 = zBack1 + tip1
            val yFront0 = y0 - 0.012f
            val yFront1 = y1 - 0.012f

            // Top curved visor skin
            addQuad(x0, y0, zBack0, x1, y1, zBack1, x1, yFront1, zFront1, x0, yFront0, zFront0, 0f, 0.96f, 0.25f, r, g, b)
            // Bottom underside
            addQuad(x1, y1 - thickness, zBack1, x0, y0 - thickness, zBack0, x0, yFront0 - thickness * 0.8f, zFront0, x1, yFront1 - thickness * 0.8f, zFront1, 0f, -0.96f, -0.25f, r * 0.72f, g * 0.72f, b * 0.72f)
            // Front rim lip
            addQuad(x0, yFront0 - thickness * 0.8f, zFront0, x1, yFront1 - thickness * 0.8f, zFront1, x1, yFront1, zFront1, x0, yFront0, zFront0, 0f, 0.1f, 0.99f, r * 0.9f, g * 0.9f, b * 0.9f)
        }
    }

    fun addHexahedron(
        bx0: Float, by0: Float, bz0: Float,
        bx1: Float, by1: Float, bz1: Float,
        bx2: Float, by2: Float, bz2: Float,
        bx3: Float, by3: Float, bz3: Float,
        tx0: Float, ty0: Float, tz0: Float,
        tx1: Float, ty1: Float, tz1: Float,
        tx2: Float, ty2: Float, tz2: Float,
        tx3: Float, ty3: Float, tz3: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        fun emitQuad(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float,
            shade: Float
        ) {
            val ux = bx - ax; val uy = by - ay; val uz = bz - az
            val vx = cx - ax; val vy = cy - ay; val vz = cz - az
            var nx = uy * vz - uz * vy
            var ny = uz * vx - ux * vz
            var nz = ux * vy - uy * vx
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            nx /= len; ny /= len; nz /= len
            addQuad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, nx, ny, nz, r * shade, g * shade, b * shade, a)
        }
        emitQuad(tx3, ty3, tz3, tx2, ty2, tz2, tx1, ty1, tz1, tx0, ty0, tz0, 1.0f)
        emitQuad(bx0, by0, bz0, bx1, by1, bz1, bx2, by2, bz2, bx3, by3, bz3, 0.72f)
        emitQuad(bx3, by3, bz3, bx2, by2, bz2, tx2, ty2, tz2, tx3, ty3, tz3, 0.96f)
        emitQuad(bx1, by1, bz1, bx0, by0, bz0, tx0, ty0, tz0, tx1, ty1, tz1, 0.88f)
        emitQuad(bx2, by2, bz2, bx1, by1, bz1, tx1, ty1, tz1, tx2, ty2, tz2, 0.91f)
        emitQuad(bx0, by0, bz0, bx3, by3, bz3, tx3, ty3, tz3, tx0, ty0, tz0, 0.91f)
    }

    fun addSlopedBox(
        cx: Float, cy: Float, cz: Float,
        bottomW: Float, bottomD: Float,
        topW: Float, topD: Float,
        height: Float,
        topShiftZ: Float = 0f,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val bw = bottomW * 0.5f; val bd = bottomD * 0.5f
        val tw = topW * 0.5f; val td = topD * 0.5f
        val y0 = cy - height * 0.5f
        val y1 = cy + height * 0.5f
        addHexahedron(
            cx - bw, y0, cz - bd, cx + bw, y0, cz - bd, cx + bw, y0, cz + bd, cx - bw, y0, cz + bd,
            cx - tw, y1, cz + topShiftZ - td, cx + tw, y1, cz + topShiftZ - td, cx + tw, y1, cz + topShiftZ + td, cx - tw, y1, cz + topShiftZ + td,
            r, g, b, a
        )
    }

    fun addBeveledBox(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        bevel: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val bAmt = bevel.coerceAtMost(minOf(sx, sy, sz) * 0.25f)
        addSlopedBox(cx, cy + sy * 0.5f - bAmt * 0.5f, cz, sx, sz, sx - bAmt * 2f, sz - bAmt * 2f, bAmt, 0f, r, g, b, a)
        addBox(cx, cy - bAmt * 0.5f, cz, sx, sy - bAmt, sz, r, g, b, a)
    }

    fun addWheelArch(
        cx: Float, cy: Float, cz: Float,
        width: Float, innerR: Float, outerR: Float,
        segs: Int = 10,
        r: Float, g: Float, b: Float
    ) {
        val hw = width * 0.5f
        for (i in 0 until segs) {
            val a0 = -PI.toFloat() * 0.44f + (i.toFloat() / segs) * (PI.toFloat() * 0.88f)
            val a1 = -PI.toFloat() * 0.44f + ((i + 1).toFloat() / segs) * (PI.toFloat() * 0.88f)
            val yIn0 = cy + cos(a0) * innerR; val zIn0 = cz + sin(a0) * innerR
            val yIn1 = cy + cos(a1) * innerR; val zIn1 = cz + sin(a1) * innerR
            val yOut0 = cy + cos(a0) * outerR; val zOut0 = cz + sin(a0) * outerR
            val yOut1 = cy + cos(a1) * outerR; val zOut1 = cz + sin(a1) * outerR
            val ny = cos((a0 + a1) * 0.5f); val nz = sin((a0 + a1) * 0.5f)
            addQuad(cx - hw, yOut0, zOut0, cx + hw, yOut0, zOut0, cx + hw, yOut1, zOut1, cx - hw, yOut1, zOut1, 0f, ny, nz, r, g, b)
            addQuad(cx + hw, yIn0, zIn0, cx - hw, yIn0, zIn0, cx - hw, yIn1, zIn1, cx + hw, yIn1, zIn1, 0f, -ny, -nz, r * 0.48f, g * 0.48f, b * 0.48f)
            addQuad(cx + hw, yIn0, zIn0, cx + hw, yOut0, zOut0, cx + hw, yOut1, zOut1, cx + hw, yIn1, zIn1, 1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
            addQuad(cx - hw, yOut0, zOut0, cx - hw, yIn0, zIn0, cx - hw, yIn1, zIn1, cx - hw, yOut1, zOut1, -1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
        }
    }

    fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        addBoxUv(cx, cy, cz, sx, sy, sz, 1f, 1f, r, g, b, a)
    }

    fun addBoxUv(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, uvScaleX: Float, uvScaleY: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        addQuadUv(x0, y1, z1, 0f, 0f, x1, y1, z1, uvScaleX, 0f, x1, y1, z0, uvScaleX, uvScaleX, x0, y1, z0, 0f, uvScaleX, 0f, 1f, 0f, r, g, b, a)
        addQuadUv(x0, y0, z0, 0f, 0f, x1, y0, z0, uvScaleX, 0f, x1, y0, z1, uvScaleX, uvScaleX, x0, y0, z1, 0f, uvScaleX, 0f, -1f, 0f, r * 0.72f, g * 0.72f, b * 0.72f, a)
        addQuadUv(x0, y0, z1, 0f, uvScaleY, x1, y0, z1, uvScaleX, uvScaleY, x1, y1, z1, uvScaleX, 0f, x0, y1, z1, 0f, 0f, 0f, 0f, 1f, r * 0.96f, g * 0.96f, b * 0.96f, a)
        addQuadUv(x1, y0, z0, 0f, uvScaleY, x0, y0, z0, uvScaleX, uvScaleY, x0, y1, z0, uvScaleX, 0f, x1, y1, z0, 0f, 0f, 0f, 0f, -1f, r * 0.88f, g * 0.88f, b * 0.88f, a)
        addQuadUv(x1, y0, z1, 0f, uvScaleY, x1, y0, z0, uvScaleX, uvScaleY, x1, y1, z0, uvScaleX, 0f, x1, y1, z1, 0f, 0f, 1f, 0f, 0f, r * 0.91f, g * 0.91f, b * 0.91f, a)
        addQuadUv(x0, y0, z0, 0f, uvScaleY, x0, y0, z1, uvScaleX, uvScaleY, x0, y1, z1, uvScaleX, 0f, x0, y1, z0, 0f, 0f, -1f, 0f, 0f, r * 0.91f, g * 0.91f, b * 0.91f, a)
    }

    fun addCone(cx: Float, baseY: Float, cz: Float, radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float) {
        val topY = baseY + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val x0 = cx + cos(a0) * radius
            val z0 = cz + sin(a0) * radius
            val x1 = cx + cos(a1) * radius
            val z1 = cz + sin(a1) * radius
            val midA = (a0 + a1) * 0.5f
            val nx = cos(midA) * 0.85f
            val nz = sin(midA) * 0.85f
            val ny = 0.52f
            addVertex(x0, baseY, z0, nx, ny, nz, 0f, 1f, r, g, b)
            addVertex(x1, baseY, z1, nx, ny, nz, 1f, 1f, r, g, b)
            addVertex(cx, topY, cz, nx, ny, nz, 0.5f, 0f, r * 1.1f, g * 1.1f, b * 1.1f)
        }
    }

    fun addCylinderY(cx: Float, baseY: Float, cz: Float, radius: Float, height: Float, segments: Int, r: Float, g: Float, b: Float) {
        addTaperedCylinderY(cx, baseY, cz, radius, radius, height, segments, r, g, b, capped = false)
    }

    fun addTaperedCylinderY(
        cx: Float,
        baseY: Float,
        cz: Float,
        bottomRadius: Float,
        topRadius: Float,
        height: Float,
        segments: Int,
        r: Float,
        g: Float,
        b: Float,
        capped: Boolean = true,
        scaleX: Float = 1f,
        scaleZ: Float = 1f
    ) {
        val topY = baseY + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val bx0 = cx + c0 * bottomRadius * scaleX; val bz0 = cz + s0 * bottomRadius * scaleZ
            val bx1 = cx + c1 * bottomRadius * scaleX; val bz1 = cz + s1 * bottomRadius * scaleZ
            val tx0 = cx + c0 * topRadius * scaleX; val tz0 = cz + s0 * topRadius * scaleZ
            val tx1 = cx + c1 * topRadius * scaleX; val tz1 = cz + s1 * topRadius * scaleZ

            addVertex(bx0, baseY, bz0, c0, 0.1f, s0, 0f, 1f, r, g, b)
            addVertex(bx1, baseY, bz1, c1, 0.1f, s1, 1f, 1f, r, g, b)
            addVertex(tx1, topY, tz1, c1, 0.1f, s1, 1f, 0f, r, g, b)

            addVertex(bx0, baseY, bz0, c0, 0.1f, s0, 0f, 1f, r, g, b)
            addVertex(tx1, topY, tz1, c1, 0.1f, s1, 1f, 0f, r, g, b)
            addVertex(tx0, topY, tz0, c0, 0.1f, s0, 0f, 0f, r, g, b)

            if (capped) {
                addVertex(cx, topY, cz, 0f, 1f, 0f, 0.5f, 0.5f, r * 1.03f, g * 1.03f, b * 1.03f)
                addVertex(tx0, topY, tz0, 0f, 1f, 0f, 0.5f, 0.5f, r * 1.03f, g * 1.03f, b * 1.03f)
                addVertex(tx1, topY, tz1, 0f, 1f, 0f, 0.5f, 0.5f, r * 1.03f, g * 1.03f, b * 1.03f)

                addVertex(cx, baseY, cz, 0f, -1f, 0f, 0.5f, 0.5f, r * 0.75f, g * 0.75f, b * 0.75f)
                addVertex(bx1, baseY, bz1, 0f, -1f, 0f, 0.5f, 0.5f, r * 0.75f, g * 0.75f, b * 0.75f)
                addVertex(bx0, baseY, bz0, 0f, -1f, 0f, 0.5f, 0.5f, r * 0.75f, g * 0.75f, b * 0.75f)
            }
        }
    }

    fun addCylinderZ(cx: Float, cy: Float, zStart: Float, zEnd: Float, radius: Float, segments: Int, r: Float, g: Float, b: Float) {
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0 = cx + c0 * radius; val y0 = cy + s0 * radius
            val x1 = cx + c1 * radius; val y1 = cy + s1 * radius

            addVertex(x0, y0, zStart, c0, s0, 0f, 0f, 0f, r, g, b)
            addVertex(x1, y1, zStart, c1, s1, 0f, 1f, 0f, r, g, b)
            addVertex(x1, y1, zEnd, c1, s1, 0f, 1f, 1f, r, g, b)

            addVertex(x0, y0, zStart, c0, s0, 0f, 0f, 0f, r, g, b)
            addVertex(x1, y1, zEnd, c1, s1, 0f, 1f, 1f, r, g, b)
            addVertex(x0, y0, zEnd, c0, s0, 0f, 0f, 1f, r, g, b)

            addVertex(cx, cy, zEnd, 0f, 0f, 1f, 0.5f, 0.5f, r * 0.95f, g * 0.95f, b * 0.95f)
            addVertex(x0, y0, zEnd, 0f, 0f, 1f, 0.5f, 0.5f, r * 0.95f, g * 0.95f, b * 0.95f)
            addVertex(x1, y1, zEnd, 0f, 0f, 1f, 0.5f, 0.5f, r * 0.95f, g * 0.95f, b * 0.95f)

            addVertex(cx, cy, zStart, 0f, 0f, -1f, 0.5f, 0.5f, r * 0.85f, g * 0.85f, b * 0.85f)
            addVertex(x1, y1, zStart, 0f, 0f, -1f, 0.5f, 0.5f, r * 0.85f, g * 0.85f, b * 0.85f)
            addVertex(x0, y0, zStart, 0f, 0f, -1f, 0.5f, 0.5f, r * 0.85f, g * 0.85f, b * 0.85f)
        }
    }

    fun addCylinderX(xStart: Float, xEnd: Float, cy: Float, cz: Float, radius: Float, segments: Int, r: Float, g: Float, b: Float) {
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val y0 = cy + c0 * radius; val z0 = cz + s0 * radius
            val y1 = cy + c1 * radius; val z1 = cz + s1 * radius

            addVertex(xStart, y0, z0, 0f, c0, s0, 0f, 0f, r, g, b)
            addVertex(xEnd, y0, z0, 0f, c0, s0, 1f, 0f, r, g, b)
            addVertex(xEnd, y1, z1, 0f, c1, s1, 1f, 1f, r, g, b)

            addVertex(xStart, y0, z0, 0f, c0, s0, 0f, 0f, r, g, b)
            addVertex(xEnd, y1, z1, 0f, c1, s1, 1f, 1f, r, g, b)
            addVertex(xStart, y1, z1, 0f, c1, s1, 0f, 1f, r, g, b)

            addVertex(xEnd, cy, cz, 1f, 0f, 0f, 0.5f, 0.5f, r * 0.92f, g * 0.92f, b * 0.92f)
            addVertex(xEnd, y0, z0, 1f, 0f, 0f, 0.5f, 0.5f, r * 0.92f, g * 0.92f, b * 0.92f)
            addVertex(xEnd, y1, z1, 1f, 0f, 0f, 0.5f, 0.5f, r * 0.92f, g * 0.92f, b * 0.92f)

            addVertex(xStart, cy, cz, -1f, 0f, 0f, 0.5f, 0.5f, r * 0.88f, g * 0.88f, b * 0.88f)
            addVertex(xStart, y1, z1, -1f, 0f, 0f, 0.5f, 0.5f, r * 0.88f, g * 0.88f, b * 0.88f)
            addVertex(xStart, y0, z0, -1f, 0f, 0f, 0.5f, 0.5f, r * 0.88f, g * 0.88f, b * 0.88f)
        }
    }

    fun addEllipsoid(
        cx: Float, cy: Float, cz: Float,
        rx: Float, ry: Float, rz: Float,
        rings: Int = 8, sectors: Int = 12,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        for (ring in 0 until rings) {
            val phi0 = (ring.toFloat() / rings) * PI.toFloat() - PI.toFloat() * 0.5f
            val phi1 = ((ring + 1).toFloat() / rings) * PI.toFloat() - PI.toFloat() * 0.5f
            val cPhi0 = cos(phi0); val sPhi0 = sin(phi0)
            val cPhi1 = cos(phi1); val sPhi1 = sin(phi1)

            for (sec in 0 until sectors) {
                val th0 = (sec.toFloat() / sectors) * 2f * PI.toFloat()
                val th1 = ((sec + 1).toFloat() / sectors) * 2f * PI.toFloat()
                val cTh0 = cos(th0); val sTh0 = sin(th0)
                val cTh1 = cos(th1); val sTh1 = sin(th1)

                val nx00 = cPhi0 * cTh0; val ny00 = sPhi0; val nz00 = cPhi0 * sTh0
                val nx10 = cPhi0 * cTh1; val ny10 = sPhi0; val nz10 = cPhi0 * sTh1
                val nx11 = cPhi1 * cTh1; val ny11 = sPhi1; val nz11 = cPhi1 * sTh1
                val nx01 = cPhi1 * cTh0; val ny01 = sPhi1; val nz01 = cPhi1 * sTh0

                addVertex(cx + nx00 * rx, cy + ny00 * ry, cz + nz00 * rz, nx00, ny00, nz00, 0f, 0f, r, g, b, a)
                addVertex(cx + nx10 * rx, cy + ny10 * ry, cz + nz10 * rz, nx10, ny10, nz10, 1f, 0f, r, g, b, a)
                addVertex(cx + nx11 * rx, cy + ny11 * ry, cz + nz11 * rz, nx11, ny11, nz11, 1f, 1f, r, g, b, a)

                addVertex(cx + nx00 * rx, cy + ny00 * ry, cz + nz00 * rz, nx00, ny00, nz00, 0f, 0f, r, g, b, a)
                addVertex(cx + nx11 * rx, cy + ny11 * ry, cz + nz11 * rz, nx11, ny11, nz11, 1f, 1f, r, g, b, a)
                addVertex(cx + nx01 * rx, cy + ny01 * ry, cz + nz01 * rz, nx01, ny01, nz01, 0f, 1f, r, g, b, a)
            }
        }
    }

    fun addTorusZ(
        cx: Float, cy: Float, cz: Float,
        majorRadius: Float, minorRadius: Float,
        majorSegs: Int = 24, minorSegs: Int = 10,
        r: Float, g: Float, b: Float
    ) {
        for (i in 0 until majorSegs) {
            val u0 = (i.toFloat() / majorSegs) * 2f * PI.toFloat()
            val u1 = ((i + 1).toFloat() / majorSegs) * 2f * PI.toFloat()
            val cu0 = cos(u0); val su0 = sin(u0)
            val cu1 = cos(u1); val su1 = sin(u1)

            for (j in 0 until minorSegs) {
                val v0 = (j.toFloat() / minorSegs) * 2f * PI.toFloat()
                val v1 = ((j + 1).toFloat() / minorSegs) * 2f * PI.toFloat()
                val cv0 = cos(v0); val sv0 = sin(v0)
                val cv1 = cos(v1); val sv1 = sin(v1)

                fun pt(cu: Float, su: Float, cv: Float, sv: Float): FloatArray {
                    val ringR = majorRadius + minorRadius * cv
                    return floatArrayOf(
                        cx + ringR * cu,
                        cy + ringR * su,
                        cz + minorRadius * sv,
                        cv * cu,
                        cv * su,
                        sv
                    )
                }

                val p00 = pt(cu0, su0, cv0, sv0)
                val p10 = pt(cu1, su1, cv0, sv0)
                val p11 = pt(cu1, su1, cv1, sv1)
                val p01 = pt(cu0, su0, cv1, sv1)

                addVertex(p00[0], p00[1], p00[2], p00[3], p00[4], p00[5], 0f, 0f, r, g, b)
                addVertex(p10[0], p10[1], p10[2], p10[3], p10[4], p10[5], 1f, 0f, r, g, b)
                addVertex(p11[0], p11[1], p11[2], p11[3], p11[4], p11[5], 1f, 1f, r, g, b)

                addVertex(p00[0], p00[1], p00[2], p00[3], p00[4], p00[5], 0f, 0f, r, g, b)
                addVertex(p11[0], p11[1], p11[2], p11[3], p11[4], p11[5], 1f, 1f, r, g, b)
                addVertex(p01[0], p01[1], p01[2], p01[3], p01[4], p01[5], 0f, 1f, r, g, b)
            }
        }
    }

    fun build(drawMode: Int = GLES30.GL_TRIANGLES): MeshBuffer {
        val arr = FloatArray(data.size)
        for (i in data.indices) arr[i] = data[i]
        return MeshBuffer(arr, drawMode)
    }
}

object WorldMeshFactory {

    fun buildUnitCube(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        mb.addBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f, 1f)
        return mb.build()
    }

    fun buildGroundQuad(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        mb.addQuadUv(
            -0.5f, 0f, 0.5f, 0f, 1f,
            0.5f, 0f, 0.5f, 1f, 1f,
            0.5f, 0f, -0.5f, 1f, 0f,
            -0.5f, 0f, -0.5f, 0f, 0f,
            0f, 1f, 0f,
            1f, 1f, 1f, 1f
        )
        return mb.build()
    }

    fun buildBillboardQuad(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        mb.addQuadUv(
            -0.5f, -0.5f, 0f, 0f, 1f,
            0.5f, -0.5f, 0f, 1f, 1f,
            0.5f, 0.5f, 0f, 1f, 0f,
            -0.5f, 0.5f, 0f, 0f, 0f,
            0f, 0f, 1f,
            1f, 1f, 1f, 1f
        )
        return mb.build()
    }

    fun buildSkyDomeMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val radius = 380f
        val rings = 8
        val sectors = 24
        for (ring in 0 until rings) {
            val p0 = (ring.toFloat() / rings) * (PI.toFloat() * 0.52f) - 0.04f
            val p1 = ((ring + 1).toFloat() / rings) * (PI.toFloat() * 0.52f) - 0.04f
            for (sec in 0 until sectors) {
                val t0 = (sec.toFloat() / sectors) * 2f * PI.toFloat()
                val t1 = ((sec + 1).toFloat() / sectors) * 2f * PI.toFloat()
                val x00 = cos(p0) * cos(t0) * radius; val y00 = sin(p0) * radius; val z00 = cos(p0) * sin(t0) * radius
                val x10 = cos(p0) * cos(t1) * radius; val y10 = sin(p0) * radius; val z10 = cos(p0) * sin(t1) * radius
                val x11 = cos(p1) * cos(t1) * radius; val y11 = sin(p1) * radius; val z11 = cos(p1) * sin(t1) * radius
                val x01 = cos(p1) * cos(t0) * radius; val y01 = sin(p1) * radius; val z01 = cos(p1) * sin(t0) * radius

                mb.addVertex(x00, y00, z00, 0f, 1f, 0f, 0f, 0f, 1f, 1f, 1f)
                mb.addVertex(x11, y11, z11, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
                mb.addVertex(x10, y10, z10, 0f, 1f, 0f, 1f, 0f, 1f, 1f, 1f)

                mb.addVertex(x00, y00, z00, 0f, 1f, 0f, 0f, 0f, 1f, 1f, 1f)
                mb.addVertex(x01, y01, z01, 0f, 1f, 0f, 0f, 1f, 1f, 1f, 1f)
                mb.addVertex(x11, y11, z11, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 1f)
            }
        }
        return mb.build()
    }

    fun buildStarFieldMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        var seed = 5511
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 10000) / 10000f
        }
        for (i in 0 until 200) {
            val az = rand() * 2f * PI.toFloat()
            val el = 0.10f + rand() * 1.18f
            val r = 340f
            val sx = cos(el) * cos(az) * r
            val sy = sin(el) * r
            val sz = cos(el) * sin(az) * r
            val szBox = 0.65f + rand() * 0.95f
            mb.addBox(sx, sy, sz, szBox, szBox, szBox, 0.92f, 0.95f, 1.0f, 0.9f)
        }
        return mb.build()
    }

    fun buildUnitWheelSet(): WheelMeshSet {
        val tireMb = DynamicMeshBuilder()
        val rimMb = DynamicMeshBuilder()
        val segs = 40
        val hw = 0.215f
        val bulgeW = 0.228f
        val bevelW = 0.165f
        val rad = 0.530f
        val grooveR = 0.517f
        val shoulderR = 0.485f
        val midSideR = 0.395f
        val rimR = 0.305f
        val midRimR = 0.215f
        val hubR = 0.115f

        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val midA = (a0 + a1) * 0.5f
            val ny = cos(midA); val nz = sin(midA)

            // Realistic matte vulcanized dark charcoal rubber tread with 3 crisp longitudinal rain channels
            val treadR = 0.195f; val treadG = 0.205f; val treadB = 0.225f
            val chR = 0.115f; val chG = 0.120f; val chB = 0.135f
            // Center & outer tread ribs
            tireMb.addQuad(-0.155f, c0 * rad, s0 * rad, -0.068f, c0 * rad, s0 * rad, -0.068f, c1 * rad, s1 * rad, -0.155f, c1 * rad, s1 * rad, 0f, ny, nz, treadR, treadG, treadB)
            tireMb.addQuad(-0.044f, c0 * rad, s0 * rad, 0.044f, c0 * rad, s0 * rad, 0.044f, c1 * rad, s1 * rad, -0.044f, c1 * rad, s1 * rad, 0f, ny, nz, treadR, treadG, treadB)
            tireMb.addQuad(0.068f, c0 * rad, s0 * rad, 0.155f, c0 * rad, s0 * rad, 0.155f, c1 * rad, s1 * rad, 0.068f, c1 * rad, s1 * rad, 0f, ny, nz, treadR, treadG, treadB)
            // Recessed circumferential rain grooves between ribs
            tireMb.addQuad(-0.068f, c0 * grooveR, s0 * grooveR, -0.044f, c0 * grooveR, s0 * grooveR, -0.044f, c1 * grooveR, s1 * grooveR, -0.068f, c1 * grooveR, s1 * grooveR, 0f, ny, nz, chR, chG, chB)
            tireMb.addQuad(0.044f, c0 * grooveR, s0 * grooveR, 0.068f, c0 * grooveR, s0 * grooveR, 0.068f, c1 * grooveR, s1 * grooveR, 0.044f, c1 * grooveR, s1 * grooveR, 0f, ny, nz, chR, chG, chB)

            // Sculpted shoulder blocks + curved bulging vulcanized rubber sidewall
            val lugShade = if (i % 2 == 0) 0.21f else 0.18f
            tireMb.addQuad(0.155f, c0 * rad, s0 * rad, bevelW, c0 * rad, s0 * rad, bevelW, c1 * rad, s1 * rad, 0.155f, c1 * rad, s1 * rad, 0f, ny, nz, lugShade, lugShade + 0.01f, lugShade + 0.02f)
            tireMb.addQuad(-bevelW, c0 * rad, s0 * rad, -0.155f, c0 * rad, s0 * rad, -0.155f, c1 * rad, s1 * rad, -bevelW, c1 * rad, s1 * rad, 0f, ny, nz, lugShade, lugShade + 0.01f, lugShade + 0.02f)

            tireMb.addQuad(bevelW, c0 * rad, s0 * rad, hw, c0 * shoulderR, s0 * shoulderR, hw, c1 * shoulderR, s1 * shoulderR, bevelW, c1 * rad, s1 * rad, 0.52f, ny * 0.84f, nz * 0.84f, 0.20f, 0.21f, 0.23f)
            tireMb.addQuad(-hw, c0 * shoulderR, s0 * shoulderR, -bevelW, c0 * rad, s0 * rad, -bevelW, c1 * rad, s1 * rad, -hw, c1 * shoulderR, s1 * shoulderR, -0.52f, ny * 0.84f, nz * 0.84f, 0.20f, 0.21f, 0.23f)

            tireMb.addQuad(hw, c0 * shoulderR, s0 * shoulderR, bulgeW, c0 * midSideR, s0 * midSideR, bulgeW, c1 * midSideR, s1 * midSideR, hw, c1 * shoulderR, s1 * shoulderR, 0.94f, ny * 0.32f, nz * 0.32f, 0.22f, 0.23f, 0.25f)
            tireMb.addQuad(-bulgeW, c0 * midSideR, s0 * midSideR, -hw, c0 * shoulderR, s0 * shoulderR, -hw, c1 * shoulderR, s1 * shoulderR, -bulgeW, c1 * midSideR, s1 * midSideR, -0.94f, ny * 0.32f, nz * 0.32f, 0.22f, 0.23f, 0.25f)

            tireMb.addQuad(bulgeW, c0 * midSideR, s0 * midSideR, hw, c0 * rimR, s0 * rimR, hw, c1 * rimR, s1 * rimR, bulgeW, c1 * midSideR, s1 * midSideR, 0.96f, -ny * 0.22f, -nz * 0.22f, 0.19f, 0.20f, 0.22f)
            tireMb.addQuad(-hw, c0 * rimR, s0 * rimR, -bulgeW, c0 * midSideR, s0 * midSideR, -bulgeW, c1 * midSideR, s1 * midSideR, -hw, c1 * rimR, s1 * rimR, -0.96f, -ny * 0.22f, -nz * 0.22f, 0.19f, 0.20f, 0.22f)

            // Separate Mirror Forged-Aluminum Rim Barrel, Hand-Hole Vents, Brake Rotor & Center Hub
            val recessX = hw * 0.52f
            rimMb.addQuad(recessX, c0 * rimR, s0 * rimR, hw, c0 * rimR, s0 * rimR, hw, c1 * rimR, s1 * rimR, recessX, c1 * rimR, s1 * rimR, 0f, -ny, -nz, 0.86f, 0.89f, 0.94f)
            rimMb.addQuad(-hw, c0 * rimR, s0 * rimR, -recessX, c0 * rimR, s0 * rimR, -recessX, c1 * rimR, s1 * rimR, -hw, c1 * rimR, s1 * rimR, 0f, -ny, -nz, 0.86f, 0.89f, 0.94f)

            // Outer rim bead lip ring + machined wheel dish
            rimMb.addQuad(recessX, c0 * midRimR, s0 * midRimR, recessX, c0 * rimR, s0 * rimR, recessX, c1 * rimR, s1 * rimR, recessX, c1 * midRimR, s1 * midRimR, 1f, 0f, 0f, 0.88f, 0.91f, 0.96f)
            rimMb.addQuad(-recessX, c0 * rimR, s0 * rimR, -recessX, c0 * midRimR, s0 * midRimR, -recessX, c1 * midRimR, s1 * midRimR, -recessX, c1 * rimR, s1 * rimR, -1f, 0f, 0f, 0.88f, 0.91f, 0.96f)

            val isVentHole = (i % 5 == 1)
            val holeFaceTone = if (isVentHole) 0.18f else 0.88f
            rimMb.addQuad(recessX, c0 * hubR, s0 * hubR, recessX, c0 * midRimR, s0 * midRimR, recessX, c1 * midRimR, s1 * midRimR, recessX, c1 * hubR, s1 * hubR, 1f, 0f, 0f, holeFaceTone, holeFaceTone + 0.02f, holeFaceTone + 0.04f)
            rimMb.addQuad(-recessX, c0 * midRimR, s0 * midRimR, -recessX, c0 * hubR, s0 * hubR, -recessX, c1 * hubR, s1 * hubR, -recessX, c1 * midRimR, s1 * midRimR, -1f, 0f, 0f, holeFaceTone, holeFaceTone + 0.02f, holeFaceTone + 0.04f)

            // Ventilated steel brake rotor + heavy-duty red brake caliper visible inside vents
            if (isVentHole) {
                val calR = if (i in 6..11) 0.88f else 0.36f
                val calG = if (i in 6..11) 0.16f else 0.38f
                val calB = if (i in 6..11) 0.11f else 0.42f
                rimMb.addQuad(recessX * 0.72f, c0 * (hubR * 1.15f), s0 * (hubR * 1.15f), recessX * 0.72f, c0 * (rimR * 0.86f), s0 * (rimR * 0.86f), recessX * 0.72f, c1 * (rimR * 0.86f), s1 * (rimR * 0.86f), recessX * 0.72f, c1 * (hubR * 1.15f), s1 * (hubR * 1.15f), 1f, 0f, 0f, calR, calG, calB)
                rimMb.addQuad(-recessX * 0.72f, c0 * (rimR * 0.86f), s0 * (rimR * 0.86f), -recessX * 0.72f, c0 * (hubR * 1.15f), s0 * (hubR * 1.15f), -recessX * 0.72f, c1 * (hubR * 1.15f), s1 * (hubR * 1.15f), -recessX * 0.72f, c1 * (rimR * 0.86f), s1 * (rimR * 0.86f), -1f, 0f, 0f, calR, calG, calB)
            }

            // Protruding chrome center axle hub bell
            val hubOut = hw + 0.04f
            rimMb.addQuad(recessX, c0 * hubR, s0 * hubR, hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), recessX, c1 * hubR, s1 * hubR, 0.45f, ny * 0.88f, nz * 0.88f, 0.88f, 0.91f, 0.96f)
            rimMb.addQuad(hubOut, 0f, 0f, hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), hubOut, 0f, 0f, 1f, 0f, 0f, 0.26f, 0.29f, 0.34f)

            rimMb.addQuad(-hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), -recessX, c0 * hubR, s0 * hubR, -recessX, c1 * hubR, s1 * hubR, -hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), -0.45f, ny * 0.88f, nz * 0.88f, 0.88f, 0.91f, 0.96f)
            rimMb.addQuad(-hubOut, 0f, 0f, -hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), -hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), -hubOut, 0f, 0f, -1f, 0f, 0f, 0.26f, 0.29f, 0.34f)
        }

        // 10 Chrome Lug Nuts around center hub
        for (k in 0 until 10) {
            val a = (k.toFloat() / 10f) * 2f * PI.toFloat()
            val ly = cos(a) * (hubR + 0.048f)
            val lz = sin(a) * (hubR + 0.048f)
            rimMb.addBox(hw * 0.65f, ly, lz, 0.045f, 0.025f, 0.025f, 0.95f, 0.97f, 1.0f)
            rimMb.addBox(-hw * 0.65f, ly, lz, 0.045f, 0.025f, 0.025f, 0.95f, 0.97f, 1.0f)
        }
        return WheelMeshSet(tireMesh = tireMb.build(), rimMesh = rimMb.build())
    }

    fun buildUnitWheel(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val segs = 32
        val hw = 0.21f
        val bevelW = 0.16f
        val rad = 0.53f
        val bevelRad = 0.48f
        val rimR = 0.305f
        val midRimR = 0.215f
        val hubR = 0.115f

        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val midA = (a0 + a1) * 0.5f
            val ny = cos(midA); val nz = sin(midA)

            // Grooved heavy-duty highway tire tread
            val treadShade = if (i % 2 == 0) 0.095f else 0.145f
            mb.addQuad(-bevelW, c0 * rad, s0 * rad, bevelW, c0 * rad, s0 * rad, bevelW, c1 * rad, s1 * rad, -bevelW, c1 * rad, s1 * rad, 0f, ny, nz, treadShade, treadShade + 0.01f, treadShade + 0.02f)

            // Curved rubber tire shoulders & ribbed sidewalls
            mb.addQuad(bevelW, c0 * rad, s0 * rad, hw, c0 * bevelRad, s0 * bevelRad, hw, c1 * bevelRad, s1 * bevelRad, bevelW, c1 * rad, s1 * rad, 0.55f, ny * 0.8f, nz * 0.8f, 0.12f, 0.13f, 0.15f)
            mb.addQuad(-hw, c0 * bevelRad, s0 * bevelRad, -bevelW, c0 * rad, s0 * rad, -bevelW, c1 * rad, s1 * rad, -hw, c1 * bevelRad, s1 * bevelRad, -0.55f, ny * 0.8f, nz * 0.8f, 0.12f, 0.13f, 0.15f)
            mb.addQuad(hw, c0 * rimR, s0 * rimR, hw, c0 * bevelRad, s0 * bevelRad, hw, c1 * bevelRad, s1 * bevelRad, hw, c1 * rimR, s1 * rimR, 1f, 0f, 0f, 0.13f, 0.14f, 0.16f)
            mb.addQuad(-hw, c0 * bevelRad, s0 * bevelRad, -hw, c0 * rimR, s0 * rimR, -hw, c1 * rimR, s1 * rimR, -hw, c1 * bevelRad, s1 * bevelRad, -1f, 0f, 0f, 0.13f, 0.14f, 0.16f)

            // Deep-dish polished forged aluminum outer rim barrel
            val recessX = hw * 0.54f
            mb.addQuad(recessX, c0 * rimR, s0 * rimR, hw, c0 * rimR, s0 * rimR, hw, c1 * rimR, s1 * rimR, recessX, c1 * rimR, s1 * rimR, 0f, -ny, -nz, 0.82f, 0.86f, 0.92f)
            mb.addQuad(-hw, c0 * rimR, s0 * rimR, -recessX, c0 * rimR, s0 * rimR, -recessX, c1 * rimR, s1 * rimR, -hw, c1 * rimR, s1 * rimR, 0f, -ny, -nz, 0.82f, 0.86f, 0.92f)

            // Outer rim flange ring + 8 machined Alcoa hand-hole cutouts exposing brake drum/caliper
            mb.addQuad(recessX, c0 * midRimR, s0 * midRimR, recessX, c0 * rimR, s0 * rimR, recessX, c1 * rimR, s1 * rimR, recessX, c1 * midRimR, s1 * midRimR, 1f, 0f, 0f, 0.86f, 0.89f, 0.94f)
            mb.addQuad(-recessX, c0 * rimR, s0 * rimR, -recessX, c0 * midRimR, s0 * midRimR, -recessX, c1 * midRimR, s1 * midRimR, -recessX, c1 * rimR, s1 * rimR, -1f, 0f, 0f, 0.86f, 0.89f, 0.94f)

            val isVentHole = (i % 4 == 1)
            val holeFaceTone = if (isVentHole) 0.14f else 0.84f
            mb.addQuad(recessX, c0 * hubR, s0 * hubR, recessX, c0 * midRimR, s0 * midRimR, recessX, c1 * midRimR, s1 * midRimR, recessX, c1 * hubR, s1 * hubR, 1f, 0f, 0f, holeFaceTone, holeFaceTone + 0.02f, holeFaceTone + 0.04f)
            mb.addQuad(-recessX, c0 * midRimR, s0 * midRimR, -recessX, c0 * hubR, s0 * hubR, -recessX, c1 * hubR, s1 * hubR, -recessX, c1 * midRimR, s1 * midRimR, -1f, 0f, 0f, holeFaceTone, holeFaceTone + 0.02f, holeFaceTone + 0.04f)

            // Recessed ventilated steel brake rotor + heavy-duty red brake caliper inside vents
            if (isVentHole) {
                val calR = if (i in 5..9) 0.84f else 0.34f
                val calG = if (i in 5..9) 0.15f else 0.36f
                val calB = if (i in 5..9) 0.10f else 0.40f
                mb.addQuad(recessX * 0.72f, c0 * (hubR * 1.15f), s0 * (hubR * 1.15f), recessX * 0.72f, c0 * (rimR * 0.86f), s0 * (rimR * 0.86f), recessX * 0.72f, c1 * (rimR * 0.86f), s1 * (rimR * 0.86f), recessX * 0.72f, c1 * (hubR * 1.15f), s1 * (hubR * 1.15f), 1f, 0f, 0f, calR, calG, calB)
                mb.addQuad(-recessX * 0.72f, c0 * (rimR * 0.86f), s0 * (rimR * 0.86f), -recessX * 0.72f, c0 * (hubR * 1.15f), s0 * (hubR * 1.15f), -recessX * 0.72f, c1 * (hubR * 1.15f), s1 * (hubR * 1.15f), -recessX * 0.72f, c1 * (rimR * 0.86f), s1 * (rimR * 0.86f), -1f, 0f, 0f, calR, calG, calB)
            }

            // Protruding chrome center axle hub cap
            val hubOut = hw + 0.04f
            mb.addQuad(recessX, c0 * hubR, s0 * hubR, hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), recessX, c1 * hubR, s1 * hubR, 0.45f, ny * 0.88f, nz * 0.88f, 0.84f, 0.87f, 0.92f)
            mb.addQuad(hubOut, 0f, 0f, hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), hubOut, 0f, 0f, 1f, 0f, 0f, 0.22f, 0.25f, 0.30f)

            mb.addQuad(-hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), -recessX, c0 * hubR, s0 * hubR, -recessX, c1 * hubR, s1 * hubR, -hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), -0.45f, ny * 0.88f, nz * 0.88f, 0.84f, 0.87f, 0.92f)
            mb.addQuad(-hubOut, 0f, 0f, -hubOut, c1 * (hubR * 0.70f), s1 * (hubR * 0.70f), -hubOut, c0 * (hubR * 0.70f), s0 * (hubR * 0.70f), -hubOut, 0f, 0f, -1f, 0f, 0f, 0.22f, 0.25f, 0.30f)
        }

        // 10 Chrome Lug Nuts around center hub
        for (k in 0 until 10) {
            val a = (k.toFloat() / 10f) * 2f * PI.toFloat()
            val ly = cos(a) * (hubR + 0.048f)
            val lz = sin(a) * (hubR + 0.048f)
            mb.addBox(hw * 0.65f, ly, lz, 0.045f, 0.025f, 0.025f, 0.94f, 0.96f, 0.99f)
            mb.addBox(-hw * 0.65f, ly, lz, 0.045f, 0.025f, 0.025f, 0.94f, 0.96f, 0.99f)
        }
        return mb.build()
    }

    private fun terrainNormalAt(x: Float, z: Float): Triple<Float, Float, Float> {
        val eps = 4.0f
        val hL = TruckWorld.terrainHeightAt(x - eps, z)
        val hR = TruckWorld.terrainHeightAt(x + eps, z)
        val hD = TruckWorld.terrainHeightAt(x, z - eps)
        val hU = TruckWorld.terrainHeightAt(x, z + eps)
        val dx = (hR - hL) / (2f * eps)
        val dz = (hU - hD) / (2f * eps)
        var nx = -dx
        var ny = 1.0f
        var nz = -dz
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)
        nx /= len; ny /= len; nz /= len
        return Triple(nx, ny, nz)
    }

    fun buildTerrainMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val grid = 112
        val n = grid + 1
        val minCoord = -1300f
        val step = 2600f / grid

        val posX = Array(n) { FloatArray(n) }
        val posY = Array(n) { FloatArray(n) }
        val posZ = Array(n) { FloatArray(n) }
        val norX = Array(n) { FloatArray(n) }
        val norY = Array(n) { FloatArray(n) }
        val norZ = Array(n) { FloatArray(n) }
        val colR = Array(n) { FloatArray(n) }
        val colG = Array(n) { FloatArray(n) }
        val colB = Array(n) { FloatArray(n) }

        for (gx in 0..grid) {
            val x = minCoord + gx * step
            for (gz in 0..grid) {
                val z = minCoord + gz * step
                val isOcean = x > 870f && z < -180f
                val isBeach = x > 820f && x <= 875f && z < -170f
                val y = if (isOcean) -0.5f else TruckWorld.terrainHeightAt(x, z)
                val (nx, ny, nz) = if (isOcean) Triple(0f, 1f, 0f) else terrainNormalAt(x, z)

                val distCity = hypot(x, z)
                val urbanBlend = (1f - ((distCity - 180f) / 145f).coerceIn(0f, 1f))
                val isSierra = z > 450f
                val steep = (1f - ny)

                val (baseR, baseG, baseB) = when {
                    isOcean -> Triple(0.08f, 0.38f, 0.66f)
                    isBeach -> Triple(0.75f, 0.67f, 0.50f)
                    else -> {
                        val rockBlend = ((steep - 0.08f) / 0.14f).coerceIn(0f, 1f)
                        val grassR = if (isSierra) 0.23f else 0.24f
                        val grassG = if (isSierra) 0.40f else 0.48f
                        val grassB = if (isSierra) 0.21f else 0.19f
                        Triple(
                            grassR + (0.42f - grassR) * rockBlend,
                            grassG + (0.39f - grassG) * rockBlend,
                            grassB + (0.36f - grassB) * rockBlend
                        )
                    }
                }
                val r = if (!isOcean && !isBeach) baseR + (0.28f - baseR) * urbanBlend * 0.72f else baseR
                val g = if (!isOcean && !isBeach) baseG + (0.31f - baseG) * urbanBlend * 0.72f else baseG
                val b = if (!isOcean && !isBeach) baseB + (0.34f - baseB) * urbanBlend * 0.72f else baseB

                posX[gx][gz] = x
                posY[gx][gz] = y
                posZ[gx][gz] = z
                norX[gx][gz] = nx
                norY[gx][gz] = ny
                norZ[gx][gz] = nz
                colR[gx][gz] = r
                colG[gx][gz] = g
                colB[gx][gz] = b
            }
        }

        val uvDiv = 14.0f
        for (gx in 0 until grid) {
            for (gz in 0 until grid) {
                val u0 = posX[gx][gz] / uvDiv
                val u1 = posX[gx + 1][gz] / uvDiv
                val v0 = posZ[gx][gz] / uvDiv
                val v1 = posZ[gx][gz + 1] / uvDiv

                // Smooth bilinear interpolated triangles (01 -> 11 -> 10, 01 -> 10 -> 00)
                mb.addVertex(posX[gx][gz + 1], posY[gx][gz + 1], posZ[gx][gz + 1], norX[gx][gz + 1], norY[gx][gz + 1], norZ[gx][gz + 1], u0, v1, colR[gx][gz + 1], colG[gx][gz + 1], colB[gx][gz + 1])
                mb.addVertex(posX[gx + 1][gz + 1], posY[gx + 1][gz + 1], posZ[gx + 1][gz + 1], norX[gx + 1][gz + 1], norY[gx + 1][gz + 1], norZ[gx + 1][gz + 1], u1, v1, colR[gx + 1][gz + 1], colG[gx + 1][gz + 1], colB[gx + 1][gz + 1])
                mb.addVertex(posX[gx + 1][gz], posY[gx + 1][gz], posZ[gx + 1][gz], norX[gx + 1][gz], norY[gx + 1][gz], norZ[gx + 1][gz], u1, v0, colR[gx + 1][gz], colG[gx + 1][gz], colB[gx + 1][gz])

                mb.addVertex(posX[gx][gz + 1], posY[gx][gz + 1], posZ[gx][gz + 1], norX[gx][gz + 1], norY[gx][gz + 1], norZ[gx][gz + 1], u0, v1, colR[gx][gz + 1], colG[gx][gz + 1], colB[gx][gz + 1])
                mb.addVertex(posX[gx + 1][gz], posY[gx + 1][gz], posZ[gx + 1][gz], norX[gx + 1][gz], norY[gx + 1][gz], norZ[gx + 1][gz], u1, v0, colR[gx + 1][gz], colG[gx + 1][gz], colB[gx + 1][gz])
                mb.addVertex(posX[gx][gz], posY[gx][gz], posZ[gx][gz], norX[gx][gz], norY[gx][gz], norZ[gx][gz], u0, v0, colR[gx][gz], colG[gx][gz], colB[gx][gz])
            }
        }
        return mb.build()
    }

    fun buildRoadsAndMarkingsMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val intersectionCoords = listOf(
            0f to 0f,
            0f to -240f,
            0f to 240f,
            240f to 0f,
            -240f to 0f,
            -680f to -520f,
            720f to -480f,
            0f to 760f
        )

        fun nearIntersection(px: Float, pz: Float): Boolean {
            for ((ix, iz) in intersectionCoords) {
                if (abs(px - ix) < 13f && abs(pz - iz) < 13f) return true
            }
            return false
        }

        TruckWorld.roads.forEachIndexed { roadIdx, road ->
            val steps = (road.length / 16f).toInt().coerceAtLeast(2)
            val rightX = -road.dirZ
            val rightZ = road.dirX
            val halfW = road.width * 0.5f
            val shoulderW = halfW + 1.5f
            val bermW = shoulderW + 2.2f
            val layerLift = (roadIdx % 5) * 0.004f

            for (i in 0 until steps) {
                val t0 = i.toFloat() / steps
                val t1 = (i + 1).toFloat() / steps
                val cx0 = road.x1 + (road.x2 - road.x1) * t0
                val cz0 = road.z1 + (road.z2 - road.z1) * t0
                val cx1 = road.x1 + (road.x2 - road.x1) * t1
                val cz1 = road.z1 + (road.z2 - road.z1) * t1

                val y0 = TruckWorld.terrainHeightAt(cx0, cz0) + 0.16f + layerLift
                val y1 = TruckWorld.terrainHeightAt(cx1, cz1) + 0.16f + layerLift

                val v0 = (t0 * road.length) / 14f
                val v1 = (t1 * road.length) / 14f

                mb.addQuad(
                    cx0 - rightX * bermW, y0 - 0.85f, cz0 - rightZ * bermW,
                    cx0 - rightX * shoulderW, y0 - 0.025f, cz0 - rightZ * shoulderW,
                    cx1 - rightX * shoulderW, y1 - 0.025f, cz1 - rightZ * shoulderW,
                    cx1 - rightX * bermW, y1 - 0.85f, cz1 - rightZ * bermW,
                    -rightX * 0.4f, 0.9f, -rightZ * 0.4f, 0.30f, 0.31f, 0.30f
                )
                mb.addQuad(
                    cx0 + rightX * shoulderW, y0 - 0.025f, cz0 + rightZ * shoulderW,
                    cx0 + rightX * bermW, y0 - 0.85f, cz0 + rightZ * bermW,
                    cx1 + rightX * bermW, y1 - 0.85f, cz1 + rightZ * bermW,
                    cx1 + rightX * shoulderW, y1 - 0.025f, cz1 + rightZ * shoulderW,
                    rightX * 0.4f, 0.9f, rightZ * 0.4f, 0.30f, 0.31f, 0.30f
                )

                mb.addQuadUv(
                    cx0 - rightX * shoulderW, y0 - 0.025f, cz0 - rightZ * shoulderW, 0f, v0,
                    cx0 + rightX * shoulderW, y0 - 0.025f, cz0 + rightZ * shoulderW, 1f, v0,
                    cx1 + rightX * shoulderW, y1 - 0.025f, cz1 + rightZ * shoulderW, 1f, v1,
                    cx1 - rightX * shoulderW, y1 - 0.025f, cz1 - rightZ * shoulderW, 0f, v1,
                    0f, 1f, 0f, 0.34f, 0.36f, 0.38f
                )

                mb.addQuadUv(
                    cx0 - rightX * halfW, y0, cz0 - rightZ * halfW, 0f, v0,
                    cx0 + rightX * halfW, y0, cz0 + rightZ * halfW, 1f, v0,
                    cx1 + rightX * halfW, y1, cz1 + rightZ * halfW, 1f, v1,
                    cx1 - rightX * halfW, y1, cz1 - rightZ * halfW, 0f, v1,
                    0f, 1f, 0f, 0.22f, 0.23f, 0.25f
                )

                if (!nearIntersection((cx0 + cx1) * 0.5f, (cz0 + cz1) * 0.5f)) {
                    val edgeInner = halfW - 0.55f
                    val edgeOuter = halfW - 0.28f
                    mb.addQuad(
                        cx0 - rightX * edgeOuter, y0 + 0.022f, cz0 - rightZ * edgeOuter,
                        cx0 - rightX * edgeInner, y0 + 0.022f, cz0 - rightZ * edgeInner,
                        cx1 - rightX * edgeInner, y1 + 0.022f, cz1 - rightZ * edgeInner,
                        cx1 - rightX * edgeOuter, y1 + 0.022f, cz1 - rightZ * edgeOuter,
                        0f, 1f, 0f, 0.90f, 0.92f, 0.94f
                    )
                    mb.addQuad(
                        cx0 + rightX * edgeInner, y0 + 0.022f, cz0 + rightZ * edgeInner,
                        cx0 + rightX * edgeOuter, y0 + 0.022f, cz0 + rightZ * edgeOuter,
                        cx1 + rightX * edgeOuter, y1 + 0.022f, cz1 + rightZ * edgeOuter,
                        cx1 + rightX * edgeInner, y1 + 0.022f, cz1 + rightZ * edgeInner,
                        0f, 1f, 0f, 0.90f, 0.92f, 0.94f
                    )

                    if (i % 2 == 0) {
                        val lineW = 0.22f
                        val (lr, lg, lb) = if (road.isHighway) Triple(0.95f, 0.74f, 0.14f) else Triple(0.90f, 0.92f, 0.95f)
                        mb.addQuad(
                            cx0 - rightX * lineW, y0 + 0.026f, cz0 - rightZ * lineW,
                            cx0 + rightX * lineW, y0 + 0.026f, cz0 + rightZ * lineW,
                            cx1 + rightX * lineW, y1 + 0.026f, cz1 + rightZ * lineW,
                            cx1 - rightX * lineW, y1 + 0.026f, cz1 - rightZ * lineW,
                            0f, 1f, 0f, lr, lg, lb
                        )
                    }
                }

                if (road.isHighway && hypot(cx0, cz0) > 250f && !nearIntersection((cx0 + cx1) * 0.5f, (cz0 + cz1) * 0.5f)) {
                    val railOff = halfW + 1.28f
                    val railHalfT = 0.08f
                    val railBot = 0.38f
                    val railTop = 0.72f
                    for (side in listOf(-1f, 1f)) {
                        val centerOff = railOff * side
                        val innerOff = centerOff - side * railHalfT
                        val outerOff = centerOff + side * railHalfT
                        // Road-facing steel W-beam face
                        mb.addQuad(
                            cx0 + rightX * innerOff, y0 + railBot, cz0 + rightZ * innerOff,
                            cx1 + rightX * innerOff, y1 + railBot, cz1 + rightZ * innerOff,
                            cx1 + rightX * innerOff, y1 + railTop, cz1 + rightZ * innerOff,
                            cx0 + rightX * innerOff, y0 + railTop, cz0 + rightZ * innerOff,
                            -rightX * side, 0.1f, -rightZ * side, 0.72f, 0.75f, 0.79f
                        )
                        // Outer shoulder-facing W-beam face
                        mb.addQuad(
                            cx1 + rightX * outerOff, y1 + railBot, cz1 + rightZ * outerOff,
                            cx0 + rightX * outerOff, y0 + railBot, cz0 + rightZ * outerOff,
                            cx0 + rightX * outerOff, y0 + railTop, cz0 + rightZ * outerOff,
                            cx1 + rightX * outerOff, y1 + railTop, cz1 + rightZ * outerOff,
                            rightX * side, 0.1f, rightZ * side, 0.58f, 0.61f, 0.66f
                        )
                        // Top rail bevel cap
                        mb.addQuad(
                            cx0 + rightX * innerOff, y0 + railTop, cz0 + rightZ * innerOff,
                            cx1 + rightX * innerOff, y1 + railTop, cz1 + rightZ * innerOff,
                            cx1 + rightX * outerOff, y1 + railTop, cz1 + rightZ * outerOff,
                            cx0 + rightX * outerOff, y0 + railTop, cz0 + rightZ * outerOff,
                            0f, 1f, 0f, 0.78f, 0.81f, 0.85f
                        )
                        if (i % 2 == 1) {
                            mb.addCylinderY(cx0 + rightX * centerOff, y0, cz0 + rightZ * centerOff, 0.085f, 0.70f, 6, 0.44f, 0.47f, 0.52f)
                            // Retro-reflective roadside safety tab
                            mb.addBox(cx0 + rightX * (innerOff - side * 0.03f), y0 + 0.60f, cz0 + rightZ * (innerOff - side * 0.03f), 0.06f, 0.11f, 0.06f, 0.98f, 0.64f, 0.10f)
                        }
                    }
                }
            }
        }

        for (tl in TruckWorld.trafficLights) {
            val iy = TruckWorld.terrainHeightAt(tl.x, tl.z) + 0.186f
            val r = 11.4f
            mb.addQuadUv(
                tl.x - r, iy, tl.z + r, 0f, 2f,
                tl.x + r, iy, tl.z + r, 2f, 2f,
                tl.x + r, iy, tl.z - r, 2f, 0f,
                tl.x - r, iy, tl.z - r, 0f, 0f,
                0f, 1f, 0f, 0.21f, 0.22f, 0.24f
            )
            for (stripe in -7..7 step 2) {
                val offset = stripe.toFloat()
                mb.addQuad(tl.x + offset - 0.5f, iy + 0.012f, tl.z + 10.6f, tl.x + offset + 0.5f, iy + 0.012f, tl.z + 10.6f, tl.x + offset + 0.5f, iy + 0.012f, tl.z + 8.4f, tl.x + offset - 0.5f, iy + 0.012f, tl.z + 8.4f, 0f, 1f, 0f, 0.92f, 0.94f, 0.96f)
                mb.addQuad(tl.x + offset - 0.5f, iy + 0.012f, tl.z - 8.4f, tl.x + offset + 0.5f, iy + 0.012f, tl.z - 8.4f, tl.x + offset + 0.5f, iy + 0.012f, tl.z - 10.6f, tl.x + offset - 0.5f, iy + 0.012f, tl.z - 10.6f, 0f, 1f, 0f, 0.92f, 0.94f, 0.96f)
                mb.addQuad(tl.x + 8.4f, iy + 0.012f, tl.z + offset + 0.5f, tl.x + 10.6f, iy + 0.012f, tl.z + offset + 0.5f, tl.x + 10.6f, iy + 0.012f, tl.z + offset - 0.5f, tl.x + 8.4f, iy + 0.012f, tl.z + offset - 0.5f, 0f, 1f, 0f, 0.92f, 0.94f, 0.96f)
                mb.addQuad(tl.x - 10.6f, iy + 0.012f, tl.z + offset + 0.5f, tl.x - 8.4f, iy + 0.012f, tl.z + offset + 0.5f, tl.x - 8.4f, iy + 0.012f, tl.z + offset - 0.5f, tl.x - 10.6f, iy + 0.012f, tl.z + offset - 0.5f, 0f, 1f, 0f, 0.92f, 0.94f, 0.96f)
            }
        }

        for (zone in TruckWorld.zones) {
            val zy = TruckWorld.terrainHeightAt(zone.x, zone.z) + 0.195f
            val r = zone.radius
            mb.addQuadUv(
                zone.x - r, zy, zone.z + r, 0f, 3f,
                zone.x + r, zy, zone.z + r, 3f, 3f,
                zone.x + r, zy, zone.z - r, 3f, 0f,
                zone.x - r, zy, zone.z - r, 0f, 0f,
                0f, 1f, 0f, 0.25f, 0.27f, 0.30f
            )
            val (br, bg, bb) = when (zone.type) {
                ZoneType.CARGO_HUB -> Triple(0.10f, 0.78f, 0.45f)
                ZoneType.GAS_STATION -> Triple(0.95f, 0.62f, 0.08f)
                ZoneType.WORKSHOP -> Triple(0.22f, 0.68f, 0.95f)
                ZoneType.PARKING -> Triple(0.60f, 0.68f, 0.80f)
            }
            val inner = r - 1.2f
            mb.addQuad(zone.x - r, zy + 0.02f, zone.z + r, zone.x + r, zy + 0.02f, zone.z + r, zone.x + r, zy + 0.02f, zone.z + inner, zone.x - r, zy + 0.02f, zone.z + inner, 0f, 1f, 0f, br, bg, bb)
            mb.addQuad(zone.x - r, zy + 0.02f, zone.z - inner, zone.x + r, zy + 0.02f, zone.z - inner, zone.x + r, zy + 0.02f, zone.z - r, zone.x - r, zy + 0.02f, zone.z - r, 0f, 1f, 0f, br, bg, bb)
        }

        return mb.build()
    }

    fun buildBuildingsMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        for (b in TruckWorld.buildings) {
            val baseY = TruckWorld.terrainHeightAt(b.x, b.z)
            val uvX = (b.width / 12f).coerceIn(1f, 4f)
            val uvY = (b.height / 16f).coerceIn(1f, 6f)
            // Main glass curtain-wall tower shaft (podium, parapet & clean concrete roof belong in buildStaticEnvironmentMesh)
            mb.addBoxUv(b.x, baseY + b.height * 0.5f, b.z, b.width, b.height, b.depth, uvX, uvY, b.colorR + 0.26f, b.colorG + 0.26f, b.colorB + 0.29f)
            if (b.isSkyscraper) {
                mb.addBoxUv(b.x, baseY + b.height + 2.2f, b.z, b.width * 0.64f, 3.0f, b.depth * 0.64f, uvX * 0.6f, 0.6f, 0.24f, 0.28f, 0.34f)
            }
        }
        return mb.build()
    }

    private fun addFractalMountain(
        mb: DynamicMeshBuilder,
        cx: Float,
        baseY: Float,
        cz: Float,
        radius: Float,
        height: Float,
        seedPhase: Float
    ) {
        val rings = 12
        val sectors = 24
        val px = Array(rings + 1) { FloatArray(sectors) }
        val py = Array(rings + 1) { FloatArray(sectors) }
        val pz = Array(rings + 1) { FloatArray(sectors) }
        val cr = Array(rings + 1) { FloatArray(sectors) }
        val cg = Array(rings + 1) { FloatArray(sectors) }
        val cb = Array(rings + 1) { FloatArray(sectors) }
        val nx = Array(rings + 1) { FloatArray(sectors) }
        val ny = Array(rings + 1) { FloatArray(sectors) }
        val nz = Array(rings + 1) { FloatArray(sectors) }

        for (ri in 0..rings) {
            val v = ri.toFloat() / rings
            val radFactor = (1f - v).pow(1.08f)
            for (s in 0 until sectors) {
                val a = (s.toFloat() / sectors) * 2f * PI.toFloat()
                // Multi-frequency ridged alpine spur displacement
                val ridge1 = 1f - abs(sin(a * 3f + seedPhase))
                val ridge2 = 1f - abs(cos(a * 5f - seedPhase * 1.7f + v * 2.4f))
                val spur = 0.78f + ridge1 * 0.28f + ridge2 * 0.14f
                val r = radius * radFactor * spur
                val hJitter = (ridge1 * 0.12f + ridge2 * 0.06f) * sin(v * PI.toFloat())
                val h = baseY + height * (v.pow(1.22f) + hJitter)

                px[ri][s] = cx + cos(a) * r
                py[ri][s] = h
                pz[ri][s] = cz + sin(a) * r

                val snowWeight = ((v - 0.60f) * 3.4f + (ridge1 - 0.5f) * 0.35f).coerceIn(0f, 1f)
                val rockWeight = ((v - 0.25f) * 2.6f).coerceIn(0f, 1f)
                val baseGrassR = 0.24f; val baseGrassG = 0.37f; val baseGrassB = 0.21f
                val graniteR = 0.40f; val graniteG = 0.41f; val graniteB = 0.44f
                val snowR = 0.92f; val snowG = 0.95f; val snowB = 0.99f

                val mr = baseGrassR + (graniteR - baseGrassR) * rockWeight
                val mg = baseGrassG + (graniteG - baseGrassG) * rockWeight
                val mbCol = baseGrassB + (graniteB - baseGrassB) * rockWeight
                cr[ri][s] = mr + (snowR - mr) * snowWeight
                cg[ri][s] = mg + (snowG - mg) * snowWeight
                cb[ri][s] = mbCol + (snowB - mbCol) * snowWeight
            }
        }

        for (ri in 0..rings) {
            val prevR = (ri - 1).coerceAtLeast(0)
            val nextR = (ri + 1).coerceAtMost(rings)
            for (s in 0 until sectors) {
                val prevS = (s - 1 + sectors) % sectors
                val nextS = (s + 1) % sectors
                val tx = px[ri][nextS] - px[ri][prevS]
                val ty = py[ri][nextS] - py[ri][prevS]
                val tz = pz[ri][nextS] - pz[ri][prevS]
                val bx = px[nextR][s] - px[prevR][s]
                val by = py[nextR][s] - py[prevR][s]
                val bz = pz[nextR][s] - pz[prevR][s]
                var vx = by * tz - bz * ty
                var vy = bz * tx - bx * tz
                var vz = bx * ty - by * tx
                val len = sqrt(vx * vx + vy * vy + vz * vz).coerceAtLeast(0.001f)
                vx /= len; vy /= len; vz /= len
                if (vy < 0f) {
                    vx = -vx; vy = -vy; vz = -vz
                }
                nx[ri][s] = vx; ny[ri][s] = vy; nz[ri][s] = vz
            }
        }

        for (ri in 0 until rings) {
            for (s in 0 until sectors) {
                val sNext = (s + 1) % sectors
                mb.addVertex(px[ri][s], py[ri][s], pz[ri][s], nx[ri][s], ny[ri][s], nz[ri][s], 0f, 0f, cr[ri][s], cg[ri][s], cb[ri][s])
                mb.addVertex(px[ri][sNext], py[ri][sNext], pz[ri][sNext], nx[ri][sNext], ny[ri][sNext], nz[ri][sNext], 1f, 0f, cr[ri][sNext], cg[ri][sNext], cb[ri][sNext])
                mb.addVertex(px[ri + 1][sNext], py[ri + 1][sNext], pz[ri + 1][sNext], nx[ri + 1][sNext], ny[ri + 1][sNext], nz[ri + 1][sNext], 1f, 1f, cr[ri + 1][sNext], cg[ri + 1][sNext], cb[ri + 1][sNext])

                mb.addVertex(px[ri][s], py[ri][s], pz[ri][s], nx[ri][s], ny[ri][s], nz[ri][s], 0f, 0f, cr[ri][s], cg[ri][s], cb[ri][s])
                mb.addVertex(px[ri + 1][sNext], py[ri + 1][sNext], pz[ri + 1][sNext], nx[ri + 1][sNext], ny[ri + 1][sNext], nz[ri + 1][sNext], 1f, 1f, cr[ri + 1][sNext], cg[ri + 1][sNext], cb[ri + 1][sNext])
                mb.addVertex(px[ri + 1][s], py[ri + 1][s], pz[ri + 1][s], nx[ri + 1][s], ny[ri + 1][s], nz[ri + 1][s], 0f, 1f, cr[ri + 1][s], cg[ri + 1][s], cb[ri + 1][s])
            }
        }
    }

    fun buildStaticEnvironmentMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()

        // Ridged Multifractal Alpine Peaks with snow caps & foothills
        TruckWorld.mountains.forEachIndexed { idx, m ->
            val my = TruckWorld.terrainHeightAt(m.x, m.z) - 2.5f
            addFractalMountain(mb, m.x, my, m.z, m.radius * 1.15f, m.height, seedPhase = idx * 1.37f)
            addFractalMountain(mb, m.x + m.radius * 0.42f, my, m.z - m.radius * 0.25f, m.radius * 0.65f, m.height * 0.64f, seedPhase = idx * 2.11f + 0.9f)
        }

        // Solid concrete city sidewalk plazas, lobby podiums, gravel roof caps, HVAC chillers & spires (whiteTex)
        for (b in TruckWorld.buildings) {
            val baseY = TruckWorld.terrainHeightAt(b.x, b.z)
            mb.addBox(b.x, baseY + 0.22f, b.z, b.width + 4.4f, 0.45f, b.depth + 4.4f, 0.32f, 0.35f, 0.40f)
            mb.addBox(b.x, baseY + 1.8f, b.z, b.width + 0.7f, 3.6f, b.depth + 0.7f, 0.17f, 0.20f, 0.25f)
            // Concrete parapet & gravel roof slab covering 100% of tower roof
            mb.addBeveledBox(b.x, baseY + b.height + 0.38f, b.z, b.width + 0.62f, 0.82f, b.depth + 0.62f, 0.15f, 0.20f, 0.23f, 0.28f)
            mb.addBox(b.x, baseY + b.height + 0.82f, b.z, b.width * 0.90f, 0.12f, b.depth * 0.90f, 0.34f, 0.37f, 0.42f)
            if (b.isSkyscraper) {
                mb.addBeveledBox(b.x, baseY + b.height + 3.85f, b.z, b.width * 0.66f, 0.45f, b.depth * 0.66f, 0.12f, 0.20f, 0.24f, 0.29f)
                mb.addBox(b.x, baseY + b.height + 4.45f, b.z, b.width * 0.36f, 1.3f, b.depth * 0.36f, 0.19f, 0.22f, 0.27f)
                mb.addBox(b.x - b.width * 0.22f, baseY + b.height + 1.25f, b.z + b.depth * 0.22f, 1.8f, 0.85f, 1.8f, 0.36f, 0.40f, 0.46f)
                mb.addCylinderY(b.x, baseY + b.height + 5.1f, b.z, 0.20f, 7.6f, 10, 0.74f, 0.77f, 0.82f)
            } else {
                mb.addBox(b.x + b.width * 0.22f, baseY + b.height + 1.22f, b.z - b.depth * 0.18f, 2.2f, 0.82f, 1.8f, 0.38f, 0.41f, 0.46f)
                mb.addCylinderY(b.x + b.width * 0.22f, baseY + b.height + 1.63f, b.z - b.depth * 0.18f, 0.55f, 0.16f, 10, 0.16f, 0.18f, 0.22f)
            }
        }

        // Highway Overhead Steel Gantry Sign Arches at city exits
        for (gantryZ in listOf(-245f, 245f)) {
            val gy = TruckWorld.terrainHeightAt(0f, gantryZ)
            mb.addCylinderY(-11.5f, gy, gantryZ, 0.30f, 7.8f, 8, 0.56f, 0.60f, 0.66f)
            mb.addCylinderY(11.5f, gy, gantryZ, 0.30f, 7.8f, 8, 0.56f, 0.60f, 0.66f)
            mb.addBox(0f, gy + 7.45f, gantryZ, 23.8f, 0.35f, 0.35f, 0.60f, 0.64f, 0.70f)
            mb.addBox(0f, gy + 6.75f, gantryZ, 23.6f, 0.32f, 0.32f, 0.56f, 0.60f, 0.66f)
            mb.addBox(0f, gy + 7.1f, gantryZ, 12.8f, 2.3f, 0.56f, 0.08f, 0.44f, 0.23f)
            mb.addBox(0f, gy + 7.75f, gantryZ + 0.31f, 9.6f, 0.28f, 0.06f, 0.94f, 0.95f, 0.97f)
            mb.addBox(-4.2f, gy + 6.6f, gantryZ + 0.31f, 2.6f, 0.42f, 0.06f, 0.96f, 0.72f, 0.08f)
            mb.addBox(3.4f, gy + 6.95f, gantryZ + 0.31f, 4.2f, 0.22f, 0.06f, 0.92f, 0.94f, 0.96f)
        }
        for (gantryX in listOf(-245f, 245f)) {
            val gy = TruckWorld.terrainHeightAt(gantryX, 0f)
            mb.addCylinderY(gantryX, gy, -11.5f, 0.30f, 7.8f, 8, 0.56f, 0.60f, 0.66f)
            mb.addCylinderY(gantryX, gy, 11.5f, 0.30f, 7.8f, 8, 0.56f, 0.60f, 0.66f)
            mb.addBox(gantryX, gy + 7.45f, 0f, 0.35f, 0.35f, 23.8f, 0.60f, 0.64f, 0.70f)
            mb.addBox(gantryX, gy + 6.75f, 0f, 0.32f, 0.32f, 23.6f, 0.56f, 0.60f, 0.66f)
            mb.addBox(gantryX, gy + 7.1f, 0f, 0.56f, 2.3f, 12.8f, 0.08f, 0.44f, 0.23f)
            mb.addBox(gantryX + 0.31f, gy + 7.75f, 0f, 0.06f, 0.28f, 9.6f, 0.94f, 0.95f, 0.97f)
        }

        for (zone in TruckWorld.zones) {
            val zy = TruckWorld.terrainHeightAt(zone.x, zone.z)
            when (zone.type) {
                ZoneType.GAS_STATION -> {
                    val cx = zone.x + 8f
                    val cz = zone.z - 8f
                    mb.addCylinderY(cx - 4.5f, zy, cz - 2f, 0.35f, 5.6f, 10, 0.85f, 0.85f, 0.88f)
                    mb.addCylinderY(cx + 4.5f, zy, cz - 2f, 0.35f, 5.6f, 10, 0.85f, 0.85f, 0.88f)
                    mb.addBeveledBox(cx, zy + 5.8f, cz, 13.4f, 0.95f, 7.6f, 0.2f, 0.96f, 0.45f, 0.05f)
                    mb.addBox(cx, zy + 5.25f, cz, 13.5f, 0.25f, 7.7f, 0.95f, 0.95f, 0.98f)
                    // Under-canopy recessed LED ceiling panels
                    mb.addBox(cx - 2.6f, zy + 5.10f, cz, 2.4f, 0.08f, 2.4f, 0.96f, 0.98f, 1.0f)
                    mb.addBox(cx + 2.6f, zy + 5.10f, cz, 2.4f, 0.08f, 2.4f, 0.96f, 0.98f, 1.0f)
                    for (px in listOf(cx - 2f, cx + 2f)) {
                        mb.addBox(px, zy + 0.25f, cz - 2f, 2.4f, 0.5f, 1.8f, 0.26f, 0.28f, 0.32f)
                        mb.addBox(px, zy + 1.30f, cz - 2f, 1.3f, 2.1f, 0.85f, 0.92f, 0.94f, 0.96f)
                        // Pump hoses & digital meter display
                        mb.addBox(px, zy + 1.45f, cz - 1.56f, 0.62f, 0.34f, 0.06f, 0.06f, 0.14f, 0.22f)
                        mb.addCylinderY(px + 0.72f, zy + 0.65f, cz - 2f, 0.045f, 1.35f, 6, 0.09f, 0.10f, 0.12f)
                        // Yellow concrete safety bollards protecting pump island
                        mb.addCylinderY(px - 1.05f, zy, cz - 2.95f, 0.12f, 1.05f, 8, 0.96f, 0.72f, 0.08f)
                        mb.addCylinderY(px + 1.05f, zy, cz - 1.05f, 0.12f, 1.05f, 8, 0.96f, 0.72f, 0.08f)
                    }
                    mb.addBox(cx - 2f, zy + 1.65f, cz - 2f, 1.34f, 0.55f, 0.88f, 0.12f, 0.65f, 0.38f)
                    mb.addBox(cx + 2f, zy + 1.65f, cz - 2f, 1.34f, 0.55f, 0.88f, 0.95f, 0.45f, 0.05f)
                    mb.addBox(cx - 9f, zy + 4.5f, cz - 5f, 2.4f, 9.0f, 0.7f, 0.14f, 0.16f, 0.20f)
                    mb.addBox(cx - 9f, zy + 7.2f, cz - 5f, 2.5f, 2.4f, 0.75f, 0.96f, 0.44f, 0.06f)
                    mb.addBox(cx - 9f, zy + 4.6f, cz - 5f, 2.2f, 1.8f, 0.76f, 0.10f, 0.14f, 0.20f)
                }
                ZoneType.WORKSHOP -> {
                    val wx = zone.x + 10f
                    val wz = zone.z + 10f
                    mb.addBox(wx, zy + 3.2f, wz, 12.5f, 6.4f, 10.5f, 0.24f, 0.32f, 0.44f)
                    mb.addBeveledBox(wx, zy + 6.6f, wz, 13.1f, 0.7f, 11.1f, 0.15f, 0.14f, 0.56f, 0.88f)
                    mb.addBox(wx, zy + 2.2f, wz - 5.3f, 6.4f, 4.4f, 0.22f, 0.15f, 0.17f, 0.21f)
                    for (ry in listOf(0.8f, 1.6f, 2.4f, 3.2f)) {
                        mb.addBox(wx, zy + ry, wz - 5.42f, 6.0f, 0.08f, 0.04f, 0.48f, 0.54f, 0.62f)
                    }
                    // Glazed upper bay door window lites + external vertical compressed-air tank
                    for (gx in listOf(-2.0f, -0.7f, 0.7f, 2.0f)) {
                        mb.addBox(wx + gx, zy + 2.8f, wz - 5.43f, 0.95f, 0.55f, 0.05f, 0.24f, 0.52f, 0.76f)
                    }
                    mb.addCylinderY(wx - 7.1f, zy + 0.2f, wz - 3.6f, 0.55f, 2.6f, 10, 0.14f, 0.52f, 0.84f)
                    mb.addBox(wx, zy + 5.2f, wz - 5.32f, 7.2f, 0.85f, 0.18f, 0.14f, 0.56f, 0.88f)
                }
                ZoneType.CARGO_HUB -> {
                    val hx = zone.x + 14f
                    val hz = zone.z + 12f
                    mb.addBox(hx, zy + 4.0f, hz, 16f, 8.0f, 12f, 0.34f, 0.38f, 0.44f)
                    mb.addBeveledBox(hx, zy + 8.3f, hz, 16.8f, 0.6f, 12.8f, 0.2f, 0.16f, 0.54f, 0.38f)
                    mb.addBox(hx - 4f, zy + 1.8f, hz - 6.2f, 5.5f, 3.6f, 0.4f, 0.15f, 0.68f, 0.42f)
                    mb.addBox(hx + 3.5f, zy + 1.8f, hz - 6.2f, 5.5f, 3.6f, 0.4f, 0.22f, 0.26f, 0.30f)
                    // Loading dock black rubber bumpers & yellow steel bollards
                    for (dx in listOf(-6.8f, -1.2f, 0.7f, 6.3f)) {
                        mb.addBox(hx + dx, zy + 0.9f, hz - 6.35f, 0.36f, 0.65f, 0.24f, 0.08f, 0.09f, 0.12f)
                        mb.addCylinderY(hx + dx, zy, hz - 6.95f, 0.14f, 1.25f, 8, 0.96f, 0.72f, 0.08f)
                    }
                    mb.addBox(hx - 14f, zy + 1.4f, hz + 1f, 2.5f, 2.8f, 6.4f, 0.82f, 0.26f, 0.16f)
                    mb.addBox(hx - 14f, zy + 4.2f, hz + 1f, 2.5f, 2.8f, 6.4f, 0.14f, 0.46f, 0.76f)
                    mb.addBox(hx - 11f, zy + 1.4f, hz + 1f, 2.5f, 2.8f, 6.4f, 0.16f, 0.58f, 0.36f)
                }
                ZoneType.PARKING -> Unit
            }
        }

        // Puerto Bahia Port Gantry Cranes & Docked Container Vessel on Coastline
        val craneX = 876f
        for (craneZ in listOf(-415f, -495f)) {
            mb.addCylinderY(craneX - 4f, -0.2f, craneZ - 4f, 0.55f, 24f, 8, 0.88f, 0.30f, 0.14f)
            mb.addCylinderY(craneX - 4f, -0.2f, craneZ + 4f, 0.55f, 24f, 8, 0.88f, 0.30f, 0.14f)
            mb.addCylinderY(craneX + 4f, -0.2f, craneZ - 4f, 0.55f, 24f, 8, 0.88f, 0.30f, 0.14f)
            mb.addCylinderY(craneX + 4f, -0.2f, craneZ + 4f, 0.55f, 24f, 8, 0.88f, 0.30f, 0.14f)
            mb.addBox(craneX + 12f, 23.5f, craneZ, 44f, 1.8f, 9f, 0.92f, 0.34f, 0.16f)
            mb.addBox(craneX + 18f, 16.5f, craneZ, 2.8f, 1.6f, 6.5f, 0.14f, 0.46f, 0.76f)
        }
        mb.addSlopedBox(924f, 2.2f, -455f, 18f, 86f, 21f, 94f, 5.4f, 0f, 0.16f, 0.20f, 0.28f)
        mb.addBox(924f, 9.2f, -486f, 15f, 9.0f, 14f, 0.92f, 0.94f, 0.96f)
        mb.addBox(924f, 11.2f, -480f, 17f, 2.4f, 6f, 0.92f, 0.94f, 0.96f)
        mb.addBox(924f, 6.4f, -438f, 14f, 3.2f, 44f, 0.82f, 0.28f, 0.18f)
        mb.addBox(920f, 9.4f, -438f, 5f, 3.0f, 36f, 0.14f, 0.52f, 0.78f)
        mb.addBox(928f, 9.4f, -442f, 5f, 3.0f, 28f, 0.18f, 0.62f, 0.42f)

        // Organic 5-tier evergreen pine boughs & clustered deciduous crowns with flared roots
        TruckWorld.trees.forEachIndexed { idx, t ->
            val ty = TruckWorld.terrainHeightAt(t.x, t.z)
            val s = t.scale
            val jx = sin(idx * 2.3f) * 0.28f * s
            val jz = cos(idx * 1.9f) * 0.28f * s
            mb.addTaperedCylinderY(t.x, ty, t.z, 0.54f * s, 0.34f * s, 1.1f * s, 8, 0.27f, 0.18f, 0.10f, capped = false)
            mb.addTaperedCylinderY(t.x, ty + 0.9f * s, t.z, 0.35f * s, 0.18f * s, 3.5f * s, 8, 0.31f, 0.20f, 0.11f, capped = false)
            if (t.isPine) {
                mb.addCone(t.x + jx * 0.3f, ty + 2.0f * s, t.z + jz * 0.3f, 2.75f * s, 3.1f * s, 10, 0.09f, 0.27f, 0.13f)
                mb.addCone(t.x - jx * 0.3f, ty + 3.4f * s, t.z + jz * 0.2f, 2.35f * s, 2.9f * s, 10, 0.11f, 0.32f, 0.15f)
                mb.addCone(t.x + jx * 0.2f, ty + 4.8f * s, t.z - jz * 0.3f, 1.90f * s, 2.7f * s, 10, 0.12f, 0.37f, 0.16f)
                mb.addCone(t.x - jx * 0.2f, ty + 6.1f * s, t.z + jz * 0.2f, 1.42f * s, 2.4f * s, 9, 0.14f, 0.42f, 0.18f)
                mb.addCone(t.x, ty + 7.4f * s, t.z, 0.90f * s, 2.1f * s, 8, 0.16f, 0.46f, 0.20f)
            } else {
                mb.addEllipsoid(t.x, ty + 5.2f * s, t.z, 2.35f * s, 1.95f * s, 2.35f * s, 7, 10, 0.16f, 0.43f, 0.15f)
                mb.addEllipsoid(t.x + 1.15f * s + jx, ty + 4.3f * s, t.z + 0.85f * s, 1.65f * s, 1.45f * s, 1.65f * s, 6, 8, 0.14f, 0.37f, 0.14f)
                mb.addEllipsoid(t.x - 1.05f * s, ty + 4.5f * s, t.z - 0.85f * s + jz, 1.65f * s, 1.42f * s, 1.65f * s, 6, 8, 0.20f, 0.48f, 0.17f)
                mb.addEllipsoid(t.x + 0.3f * s, ty + 6.3f * s, t.z - 0.3f * s, 1.38f * s, 1.15f * s, 1.38f * s, 6, 8, 0.24f, 0.54f, 0.18f)
            }
        }

        for (d in -200..200 step 50) {
            if (d == 0) continue
            addStreetlightPole(mb, d.toFloat(), 11.5f)
            addStreetlightPole(mb, 11.5f, d.toFloat())
        }

        for (sign in TruckWorld.roadSigns) {
            val sy = TruckWorld.terrainHeightAt(sign.x, sign.z)
            mb.addCylinderY(sign.x, sy, sign.z, 0.13f, 3.6f, 8, 0.62f, 0.66f, 0.72f)
            val (sr, sg, sb) = when (sign.type) {
                com.example.sim.SignType.STOP -> Triple(0.86f, 0.12f, 0.12f)
                com.example.sim.SignType.FUEL_AHEAD -> Triple(0.12f, 0.48f, 0.88f)
                com.example.sim.SignType.CURVE -> Triple(0.95f, 0.72f, 0.08f)
                else -> Triple(0.92f, 0.94f, 0.96f)
            }
            // Reflective white/steel outer plate + inner colored sign face
            mb.addBox(sign.x, sy + 3.5f, sign.z, 1.32f, 1.32f, 0.12f, 0.88f, 0.90f, 0.94f)
            mb.addBox(sign.x, sy + 3.5f, sign.z, 1.12f, 1.12f, 0.16f, sr, sg, sb)
            if (sign.type == com.example.sim.SignType.STOP) {
                mb.addBox(sign.x, sy + 3.5f, sign.z, 0.74f, 0.22f, 0.19f, 0.96f, 0.97f, 0.99f)
            } else {
                mb.addBox(sign.x, sy + 3.5f, sign.z, 0.34f, 0.56f, 0.19f, 0.14f, 0.16f, 0.20f)
            }
        }

        return mb.build()
    }

    private fun addStreetlightPole(mb: DynamicMeshBuilder, x: Float, z: Float) {
        val y = TruckWorld.terrainHeightAt(x, z)
        mb.addTaperedCylinderY(x, y, z, 0.24f, 0.14f, 0.55f, 8, 0.34f, 0.37f, 0.42f, capped = false)
        mb.addTaperedCylinderY(x, y + 0.55f, z, 0.14f, 0.10f, 6.7f, 8, 0.46f, 0.49f, 0.55f, capped = false)
        mb.addSlopedBox(x, y + 7.25f, z - 0.8f, 0.18f, 0.18f, 0.24f, 1.85f, 0.22f, -0.35f, 0.50f, 0.54f, 0.60f)
        mb.addBeveledBox(x, y + 7.12f, z - 1.55f, 0.52f, 0.16f, 0.74f, 0.05f, 0.36f, 0.40f, 0.46f)
    }

    fun buildNightWindowsAndLightsMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        for ((idx, b) in TruckWorld.buildings.withIndex()) {
            val baseY = TruckWorld.terrainHeightAt(b.x, b.z)
            if (b.isSkyscraper) {
                // Crisp aviation warning beacon on skyscraper spire tip
                mb.addBox(b.x, baseY + b.height + 12.6f, b.z, 0.52f, 0.52f, 0.52f, 1.0f, 0.14f, 0.10f)
            }
            val rows = ((b.height - 5f) / 4.2f).toInt().coerceIn(1, 9)
            val colsX = (b.width / 3.4f).toInt().coerceIn(2, 6)
            val colsZ = (b.depth / 3.4f).toInt().coerceIn(2, 6)
            val halfW = b.width * 0.5f + 0.05f
            val halfD = b.depth * 0.5f + 0.05f

            for (r in 1..rows) {
                val wy = baseY + 3.4f + r * 3.8f
                if (wy > baseY + b.height - 1.5f) continue
                for (c in 0 until colsX) {
                    val h = (idx * 31 + r * 17 + c * 13) and 0x7f
                    if (h % 5 <= 1) {
                        val wx = b.x - b.width * 0.36f + (c.toFloat() / (colsX - 1).coerceAtLeast(1)) * (b.width * 0.72f)
                        val warm = h % 3 != 0
                        val lr = if (warm) 1.0f else 0.72f
                        val lg = if (warm) 0.86f else 0.88f
                        val lb = if (warm) 0.58f else 1.0f
                        mb.addBox(wx, wy, b.z + halfD, 1.35f, 1.15f, 0.06f, lr, lg, lb)
                        if (h % 2 == 0) {
                            mb.addBox(wx, wy, b.z - halfD, 1.35f, 1.15f, 0.06f, lr, lg, lb)
                        }
                    }
                }
                for (c in 0 until colsZ) {
                    val h = (idx * 23 + r * 19 + c * 11) and 0x7f
                    if (h % 5 <= 1) {
                        val wz = b.z - b.depth * 0.36f + (c.toFloat() / (colsZ - 1).coerceAtLeast(1)) * (b.depth * 0.72f)
                        val warm = h % 4 != 0
                        val lr = if (warm) 1.0f else 0.72f
                        val lg = if (warm) 0.86f else 0.88f
                        val lb = if (warm) 0.58f else 1.0f
                        mb.addBox(b.x + halfW, wy, wz, 0.06f, 1.15f, 1.35f, lr, lg, lb)
                        if (h % 2 == 1) {
                            mb.addBox(b.x - halfW, wy, wz, 0.06f, 1.15f, 1.35f, lr, lg, lb)
                        }
                    }
                }
            }
        }
        for (d in -200..200 step 50) {
            if (d == 0) continue
            val y1 = TruckWorld.terrainHeightAt(d.toFloat(), 11.5f)
            mb.addBox(d.toFloat(), y1 + 7.0f, 10.1f, 0.45f, 0.18f, 0.65f, 1.0f, 0.90f, 0.65f)
            val y2 = TruckWorld.terrainHeightAt(11.5f, d.toFloat())
            mb.addBox(11.5f, y2 + 7.0f, d.toFloat() - 1.4f, 0.45f, 0.18f, 0.65f, 1.0f, 0.90f, 0.65f)
        }
        return mb.build()
    }

    /**
     * Single-draw batched diagonal rain streak volume tile (36m x 22m x 36m) for rain weather.
     */
    fun buildRainVolumeMesh(streakCount: Int = 360): MeshBuffer {
        val mb = DynamicMeshBuilder()
        var seed = 99173
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 10000) / 10000f
        }
        for (i in 0 until streakCount) {
            val rx = (rand() - 0.5f) * 38f
            val ry = rand() * 22f
            val rz = (rand() - 0.5f) * 38f
            val len = 0.75f + rand() * 0.55f
            val slant = 0.10f
            // Crossed pair of ultra-thin translucent rain ribbons so visible from every camera angle
            mb.addQuad(
                rx - 0.015f, ry, rz,
                rx + 0.015f, ry, rz,
                rx + 0.015f + slant, ry + len, rz,
                rx - 0.015f + slant, ry + len, rz,
                0f, 0f, 1f, 0.78f, 0.88f, 1.0f, 0.42f
            )
            mb.addQuad(
                rx, ry, rz - 0.015f,
                rx, ry, rz + 0.015f,
                rx + slant, ry + len, rz + 0.015f,
                rx + slant, ry + len, rz - 0.015f,
                1f, 0f, 0f, 0.78f, 0.88f, 1.0f, 0.42f
            )
        }
        return mb.build()
    }

    fun buildScreenQuad(): ScreenQuadBuffer {
        val verts = floatArrayOf(
            0f, 0f, 0f, 0f,
            1f, 0f, 1f, 0f,
            1f, 1f, 1f, 1f,
            0f, 0f, 0f, 0f,
            1f, 1f, 1f, 1f,
            0f, 1f, 0f, 1f
        )
        val vaos = IntArray(1)
        val vbos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        GLES30.glGenBuffers(1, vbos, 0)
        val fb = ByteBuffer.allocateDirect(verts.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(verts)
        fb.position(0)
        GLES30.glBindVertexArray(vaos[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbos[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, verts.size * 4, fb, GLES30.GL_STATIC_DRAW)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
        GLES30.glBindVertexArray(0)
        return ScreenQuadBuffer(vaos[0], vbos[0])
    }
}

class ScreenQuadBuffer(private val vaoId: Int, private val vboId: Int) {
    fun draw() {
        GLES30.glBindVertexArray(vaoId)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(1, intArrayOf(vboId), 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vaoId), 0)
    }
}
