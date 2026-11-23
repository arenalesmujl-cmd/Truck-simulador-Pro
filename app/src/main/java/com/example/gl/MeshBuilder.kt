package com.example.gl

import android.opengl.GLES30
import com.example.sim.TruckWorld
import com.example.sim.ZoneType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

class MeshBuffer(
    vertices: FloatArray,
    private val drawMode: Int = GLES30.GL_TRIANGLES
) {
    // Stride: pos(3) + normal(3) + color(4) = 10 floats = 40 bytes
    val vertexCount: Int = vertices.size / 10
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

        val strideBytes = 10 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, strideBytes, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, strideBytes, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, strideBytes, 6 * 4)

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
    private val data = ArrayList<Float>(8192)

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b); data.add(a)
    }

    fun addQuad(
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        x4: Float, y4: Float, z4: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
        addVertex(x2, y2, z2, nx, ny, nz, r, g, b, a)
        addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)

        addVertex(x1, y1, z1, nx, ny, nz, r, g, b, a)
        addVertex(x3, y3, z3, nx, ny, nz, r, g, b, a)
        addVertex(x4, y4, z4, nx, ny, nz, r, g, b, a)
    }

    /**
     * Adds an arbitrary 8-corner hexahedron (perfect for sloped hoods, windshields,
     * wheel arches, fenders, and aerodynamic roofs).
     * Bottom 4 corners: b0(-x,-z), b1(+x,-z), b2(+x,+z), b3(-x,+z)
     * Top 4 corners:    t0(-x,-z), t1(+x,-z), t2(+x,+z), t3(-x,+z)
     */
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
        // Top
        emitQuad(tx3, ty3, tz3, tx2, ty2, tz2, tx1, ty1, tz1, tx0, ty0, tz0, 1.0f)
        // Bottom
        emitQuad(bx0, by0, bz0, bx1, by1, bz1, bx2, by2, bz2, bx3, by3, bz3, 0.72f)
        // Front (+Z)
        emitQuad(bx3, by3, bz3, bx2, by2, bz2, tx2, ty2, tz2, tx3, ty3, tz3, 0.96f)
        // Back (-Z)
        emitQuad(bx1, by1, bz1, bx0, by0, bz0, tx0, ty0, tz0, tx1, ty1, tz1, 0.88f)
        // Right (+X)
        emitQuad(bx2, by2, bz2, bx1, by1, bz1, tx1, ty1, tz1, tx2, ty2, tz2, 0.91f)
        // Left (-X)
        emitQuad(bx0, by0, bz0, bx3, by3, bz3, tx3, ty3, tz3, tx0, ty0, tz0, 0.91f)
    }

    /**
     * Sloped rectangular box where the top face can be narrower or shifted along Z/X.
     */
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

    fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, r, g, b, a)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r * 0.72f, g * 0.72f, b * 0.72f, a)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r * 0.96f, g * 0.96f, b * 0.96f, a)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r * 0.88f, g * 0.88f, b * 0.88f, a)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r * 0.91f, g * 0.91f, b * 0.91f, a)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r * 0.91f, g * 0.91f, b * 0.91f, a)
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
            addVertex(x0, baseY, z0, nx, ny, nz, r, g, b)
            addVertex(x1, baseY, z1, nx, ny, nz, r, g, b)
            addVertex(cx, topY, cz, nx, ny, nz, r * 1.1f, g * 1.1f, b * 1.1f)
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

            addVertex(bx0, baseY, bz0, c0, 0.1f, s0, r, g, b)
            addVertex(bx1, baseY, bz1, c1, 0.1f, s1, r, g, b)
            addVertex(tx1, topY, tz1, c1, 0.1f, s1, r, g, b)

            addVertex(bx0, baseY, bz0, c0, 0.1f, s0, r, g, b)
            addVertex(tx1, topY, tz1, c1, 0.1f, s1, r, g, b)
            addVertex(tx0, topY, tz0, c0, 0.1f, s0, r, g, b)

            if (capped) {
                addVertex(cx, topY, cz, 0f, 1f, 0f, r * 1.03f, g * 1.03f, b * 1.03f)
                addVertex(tx0, topY, tz0, 0f, 1f, 0f, r * 1.03f, g * 1.03f, b * 1.03f)
                addVertex(tx1, topY, tz1, 0f, 1f, 0f, r * 1.03f, g * 1.03f, b * 1.03f)

                addVertex(cx, baseY, cz, 0f, -1f, 0f, r * 0.75f, g * 0.75f, b * 0.75f)
                addVertex(bx1, baseY, bz1, 0f, -1f, 0f, r * 0.75f, g * 0.75f, b * 0.75f)
                addVertex(bx0, baseY, bz0, 0f, -1f, 0f, r * 0.75f, g * 0.75f, b * 0.75f)
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

            addVertex(x0, y0, zStart, c0, s0, 0f, r, g, b)
            addVertex(x1, y1, zStart, c1, s1, 0f, r, g, b)
            addVertex(x1, y1, zEnd, c1, s1, 0f, r, g, b)

            addVertex(x0, y0, zStart, c0, s0, 0f, r, g, b)
            addVertex(x1, y1, zEnd, c1, s1, 0f, r, g, b)
            addVertex(x0, y0, zEnd, c0, s0, 0f, r, g, b)

            // Front cap (+Z)
            addVertex(cx, cy, zEnd, 0f, 0f, 1f, r * 0.95f, g * 0.95f, b * 0.95f)
            addVertex(x0, y0, zEnd, 0f, 0f, 1f, r * 0.95f, g * 0.95f, b * 0.95f)
            addVertex(x1, y1, zEnd, 0f, 0f, 1f, r * 0.95f, g * 0.95f, b * 0.95f)

            // Back cap (-Z)
            addVertex(cx, cy, zStart, 0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f)
            addVertex(x1, y1, zStart, 0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f)
            addVertex(x0, y0, zStart, 0f, 0f, -1f, r * 0.85f, g * 0.85f, b * 0.85f)
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

            addVertex(xStart, y0, z0, 0f, c0, s0, r, g, b)
            addVertex(xEnd, y0, z0, 0f, c0, s0, r, g, b)
            addVertex(xEnd, y1, z1, 0f, c1, s1, r, g, b)

            addVertex(xStart, y0, z0, 0f, c0, s0, r, g, b)
            addVertex(xEnd, y1, z1, 0f, c1, s1, r, g, b)
            addVertex(xStart, y1, z1, 0f, c1, s1, r, g, b)

            addVertex(xEnd, cy, cz, 1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
            addVertex(xEnd, y0, z0, 1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)
            addVertex(xEnd, y1, z1, 1f, 0f, 0f, r * 0.92f, g * 0.92f, b * 0.92f)

            addVertex(xStart, cy, cz, -1f, 0f, 0f, r * 0.88f, g * 0.88f, b * 0.88f)
            addVertex(xStart, y1, z1, -1f, 0f, 0f, r * 0.88f, g * 0.88f, b * 0.88f)
            addVertex(xStart, y0, z0, -1f, 0f, 0f, r * 0.88f, g * 0.88f, b * 0.88f)
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

                addVertex(cx + nx00 * rx, cy + ny00 * ry, cz + nz00 * rz, nx00, ny00, nz00, r, g, b, a)
                addVertex(cx + nx10 * rx, cy + ny10 * ry, cz + nz10 * rz, nx10, ny10, nz10, r, g, b, a)
                addVertex(cx + nx11 * rx, cy + ny11 * ry, cz + nz11 * rz, nx11, ny11, nz11, r, g, b, a)

                addVertex(cx + nx00 * rx, cy + ny00 * ry, cz + nz00 * rz, nx00, ny00, nz00, r, g, b, a)
                addVertex(cx + nx11 * rx, cy + ny11 * ry, cz + nz11 * rz, nx11, ny11, nz11, r, g, b, a)
                addVertex(cx + nx01 * rx, cy + ny01 * ry, cz + nz01 * rz, nx01, ny01, nz01, r, g, b, a)
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

                addVertex(p00[0], p00[1], p00[2], p00[3], p00[4], p00[5], r, g, b)
                addVertex(p10[0], p10[1], p10[2], p10[3], p10[4], p10[5], r, g, b)
                addVertex(p11[0], p11[1], p11[2], p11[3], p11[4], p11[5], r, g, b)

                addVertex(p00[0], p00[1], p00[2], p00[3], p00[4], p00[5], r, g, b)
                addVertex(p11[0], p11[1], p11[2], p11[3], p11[4], p11[5], r, g, b)
                addVertex(p01[0], p01[1], p01[2], p01[3], p01[4], p01[5], r, g, b)
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

                mb.addVertex(x00, y00, z00, 0f, 1f, 0f, 1f, 1f, 1f)
                mb.addVertex(x11, y11, z11, 0f, 1f, 0f, 1f, 1f, 1f)
                mb.addVertex(x10, y10, z10, 0f, 1f, 0f, 1f, 1f, 1f)

                mb.addVertex(x00, y00, z00, 0f, 1f, 0f, 1f, 1f, 1f)
                mb.addVertex(x01, y01, z01, 0f, 1f, 0f, 1f, 1f, 1f)
                mb.addVertex(x11, y11, z11, 0f, 1f, 0f, 1f, 1f, 1f)
            }
        }
        return mb.build()
    }

    /**
     * High-detail truck wheel with beveled rubber shoulder, tread ribs, recessed alloy rim,
     * 10 chrome lug nuts, and hub cap.
     */
    fun buildUnitWheel(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val segs = 22
        val hw = 0.21f
        val bevelW = 0.16f
        val rad = 0.53f
        val bevelRad = 0.48f
        val rimR = 0.29f
        val hubR = 0.11f

        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val midA = (a0 + a1) * 0.5f
            val ny = cos(midA); val nz = sin(midA)

            // Outer tread (alternating subtle lug rib shade for visible tire rotation!)
            val treadShade = if (i % 2 == 0) 0.12f else 0.16f
            mb.addQuad(-bevelW, c0 * rad, s0 * rad, bevelW, c0 * rad, s0 * rad, bevelW, c1 * rad, s1 * rad, -bevelW, c1 * rad, s1 * rad, 0f, ny, nz, treadShade, treadShade + 0.01f, treadShade + 0.02f)

            // Tire bevel shoulders
            mb.addQuad(bevelW, c0 * rad, s0 * rad, hw, c0 * bevelRad, s0 * bevelRad, hw, c1 * bevelRad, s1 * bevelRad, bevelW, c1 * rad, s1 * rad, 0.55f, ny * 0.8f, nz * 0.8f, 0.14f, 0.15f, 0.17f)
            mb.addQuad(-hw, c0 * bevelRad, s0 * bevelRad, -bevelW, c0 * rad, s0 * rad, -bevelW, c1 * rad, s1 * rad, -hw, c1 * bevelRad, s1 * bevelRad, -0.55f, ny * 0.8f, nz * 0.8f, 0.14f, 0.15f, 0.17f)

            // Tire sidewalls
            mb.addQuad(hw, c0 * rimR, s0 * rimR, hw, c0 * bevelRad, s0 * bevelRad, hw, c1 * bevelRad, s1 * bevelRad, hw, c1 * rimR, s1 * rimR, 1f, 0f, 0f, 0.15f, 0.16f, 0.18f)
            mb.addQuad(-hw, c0 * bevelRad, s0 * bevelRad, -hw, c0 * rimR, s0 * rimR, -hw, c1 * rimR, s1 * rimR, -hw, c1 * bevelRad, s1 * bevelRad, -1f, 0f, 0f, 0.15f, 0.16f, 0.18f)

            // Deep-dish polished alloy rim barrel
            val recessX = hw * 0.62f
            mb.addQuad(recessX, c0 * rimR, s0 * rimR, hw, c0 * rimR, s0 * rimR, hw, c1 * rimR, s1 * rimR, recessX, c1 * rimR, s1 * rimR, 0f, -ny, -nz, 0.72f, 0.76f, 0.82f)
            mb.addQuad(-hw, c0 * rimR, s0 * rimR, -recessX, c0 * rimR, s0 * rimR, -recessX, c1 * rimR, s1 * rimR, -hw, c1 * rimR, s1 * rimR, 0f, -ny, -nz, 0.72f, 0.76f, 0.82f)

            // Rim dish face + ventilated cooling holes accent
            val faceTone = if (i % 2 == 0) 0.80f else 0.62f
            mb.addQuad(recessX, c0 * hubR, s0 * hubR, recessX, c0 * rimR, s0 * rimR, recessX, c1 * rimR, s1 * rimR, recessX, c1 * hubR, s1 * hubR, 1f, 0f, 0f, faceTone, faceTone + 0.02f, faceTone + 0.05f)
            mb.addQuad(-recessX, c0 * rimR, s0 * rimR, -recessX, c0 * hubR, s0 * hubR, -recessX, c1 * hubR, s1 * hubR, -recessX, c1 * rimR, s1 * rimR, -1f, 0f, 0f, faceTone, faceTone + 0.02f, faceTone + 0.05f)

            // Protruding central axle hub
            val hubOut = hw + 0.03f
            mb.addQuad(recessX, c0 * hubR, s0 * hubR, hubOut, c0 * (hubR * 0.7f), s0 * (hubR * 0.7f), hubOut, c1 * (hubR * 0.7f), s1 * (hubR * 0.7f), recessX, c1 * hubR, s1 * hubR, 0.4f, ny * 0.9f, nz * 0.9f, 0.75f, 0.78f, 0.84f)
            mb.addQuad(hubOut, 0f, 0f, hubOut, c0 * (hubR * 0.7f), s0 * (hubR * 0.7f), hubOut, c1 * (hubR * 0.7f), s1 * (hubR * 0.7f), hubOut, 0f, 0f, 1f, 0f, 0f, 0.25f, 0.28f, 0.32f)

            mb.addQuad(-hubOut, c0 * (hubR * 0.7f), s0 * (hubR * 0.7f), -recessX, c0 * hubR, s0 * hubR, -recessX, c1 * hubR, s1 * hubR, -hubOut, c1 * (hubR * 0.7f), s1 * (hubR * 0.7f), -0.4f, ny * 0.9f, nz * 0.9f, 0.75f, 0.78f, 0.84f)
            mb.addQuad(-hubOut, 0f, 0f, -hubOut, c1 * (hubR * 0.7f), s1 * (hubR * 0.7f), -hubOut, c0 * (hubR * 0.7f), s0 * (hubR * 0.7f), -hubOut, 0f, 0f, -1f, 0f, 0f, 0.25f, 0.28f, 0.32f)
        }

        // 10 Chrome Lug Nuts on both sides
        for (k in 0 until 10) {
            val a = (k.toFloat() / 10f) * 2f * PI.toFloat()
            val ly = cos(a) * (hubR + 0.055f)
            val lz = sin(a) * (hubR + 0.055f)
            mb.addBox(hw * 0.68f, ly, lz, 0.04f, 0.025f, 0.025f, 0.92f, 0.94f, 0.96f)
            mb.addBox(-hw * 0.68f, ly, lz, 0.04f, 0.025f, 0.025f, 0.92f, 0.94f, 0.96f)
        }
        return mb.build()
    }

    fun buildTerrainMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val grid = 52
        val minCoord = -1300f
        val step = 2600f / grid

        for (gx in 0 until grid) {
            for (gz in 0 until grid) {
                val x0 = minCoord + gx * step
                val x1 = x0 + step
                val z0 = minCoord + gz * step
                val z1 = z0 + step
                val cx = (x0 + x1) * 0.5f
                val cz = (z0 + z1) * 0.5f

                val y00 = TruckWorld.terrainHeightAt(x0, z0)
                val y10 = TruckWorld.terrainHeightAt(x1, z0)
                val y11 = TruckWorld.terrainHeightAt(x1, z1)
                val y01 = TruckWorld.terrainHeightAt(x0, z1)

                val isOcean = cx > 870f && cz < -180f
                val isUrban = hypot(cx, cz) < 280f
                val isSierra = cz > 450f

                val (r, g, b) = when {
                    isOcean -> Triple(0.08f, 0.32f, 0.54f)
                    isUrban -> Triple(0.24f, 0.26f, 0.28f)
                    isSierra -> Triple(0.24f, 0.36f, 0.20f)
                    (gx + gz) % 3 == 0 -> Triple(0.18f, 0.42f, 0.16f)
                    else -> Triple(0.22f, 0.46f, 0.18f)
                }

                val h00 = if (isOcean) -0.5f else y00
                val h10 = if (isOcean) -0.5f else y10
                val h11 = if (isOcean) -0.5f else y11
                val h01 = if (isOcean) -0.5f else y01

                mb.addQuad(x0, h01, z1, x1, h11, z1, x1, h10, z0, x0, h00, z0, 0f, 1f, 0f, r, g, b)
            }
        }
        return mb.build()
    }

    fun buildRoadsAndMarkingsMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        for (road in TruckWorld.roads) {
            val steps = (road.length / 22f).toInt().coerceAtLeast(2)
            val rightX = -road.dirZ
            val rightZ = road.dirX
            val halfW = road.width * 0.5f
            val shoulderW = halfW + 1.5f

            for (i in 0 until steps) {
                val t0 = i.toFloat() / steps
                val t1 = (i + 1).toFloat() / steps
                val cx0 = road.x1 + (road.x2 - road.x1) * t0
                val cz0 = road.z1 + (road.z2 - road.z1) * t0
                val cx1 = road.x1 + (road.x2 - road.x1) * t1
                val cz1 = road.z1 + (road.z2 - road.z1) * t1

                val y0 = TruckWorld.terrainHeightAt(cx0, cz0) + 0.06f
                val y1 = TruckWorld.terrainHeightAt(cx1, cz1) + 0.06f

                mb.addQuad(
                    cx0 - rightX * shoulderW, y0 - 0.02f, cz0 - rightZ * shoulderW,
                    cx0 + rightX * shoulderW, y0 - 0.02f, cz0 + rightZ * shoulderW,
                    cx1 + rightX * shoulderW, y1 - 0.02f, cz1 + rightZ * shoulderW,
                    cx1 - rightX * shoulderW, y1 - 0.02f, cz1 - rightZ * shoulderW,
                    0f, 1f, 0f, 0.34f, 0.35f, 0.37f
                )

                val shade = if (i % 2 == 0) 0.14f else 0.16f
                mb.addQuad(
                    cx0 - rightX * halfW, y0, cz0 - rightZ * halfW,
                    cx0 + rightX * halfW, y0, cz0 + rightZ * halfW,
                    cx1 + rightX * halfW, y1, cz1 + rightZ * halfW,
                    cx1 - rightX * halfW, y1, cz1 - rightZ * halfW,
                    0f, 1f, 0f, shade, shade + 0.01f, shade + 0.02f
                )

                val edgeInner = halfW - 0.55f
                val edgeOuter = halfW - 0.28f
                mb.addQuad(
                    cx0 - rightX * edgeOuter, y0 + 0.02f, cz0 - rightZ * edgeOuter,
                    cx0 - rightX * edgeInner, y0 + 0.02f, cz0 - rightZ * edgeInner,
                    cx1 - rightX * edgeInner, y1 + 0.02f, cz1 - rightZ * edgeInner,
                    cx1 - rightX * edgeOuter, y1 + 0.02f, cz1 - rightZ * edgeOuter,
                    0f, 1f, 0f, 0.90f, 0.92f, 0.94f
                )
                mb.addQuad(
                    cx0 + rightX * edgeInner, y0 + 0.02f, cz0 + rightZ * edgeInner,
                    cx0 + rightX * edgeOuter, y0 + 0.02f, cz0 + rightZ * edgeOuter,
                    cx1 + rightX * edgeOuter, y1 + 0.02f, cz1 + rightZ * edgeOuter,
                    cx1 + rightX * edgeInner, y1 + 0.02f, cz1 + rightZ * edgeInner,
                    0f, 1f, 0f, 0.90f, 0.92f, 0.94f
                )

                if (i % 2 == 0) {
                    val lineW = 0.22f
                    val (lr, lg, lb) = if (road.isHighway) Triple(0.95f, 0.74f, 0.14f) else Triple(0.90f, 0.92f, 0.95f)
                    mb.addQuad(
                        cx0 - rightX * lineW, y0 + 0.025f, cz0 - rightZ * lineW,
                        cx0 + rightX * lineW, y0 + 0.025f, cz0 + rightZ * lineW,
                        cx1 + rightX * lineW, y1 + 0.025f, cz1 + rightZ * lineW,
                        cx1 - rightX * lineW, y1 + 0.025f, cz1 - rightZ * lineW,
                        0f, 1f, 0f, lr, lg, lb
                    )
                }

                // Highway steel guardrails every 2nd segment outside Central
                if (road.isHighway && i % 2 == 1 && hypot(cx0, cz0) > 250f) {
                    val railOff = halfW + 1.25f
                    mb.addBox(cx0 + rightX * railOff, y0 + 0.52f, cz0 + rightZ * railOff, 0.18f, 0.35f, 14f, 0.68f, 0.70f, 0.74f)
                    mb.addBox(cx0 - rightX * railOff, y0 + 0.52f, cz0 - rightZ * railOff, 0.18f, 0.35f, 14f, 0.68f, 0.70f, 0.74f)
                }
            }
        }

        for (zone in TruckWorld.zones) {
            val zy = TruckWorld.terrainHeightAt(zone.x, zone.z) + 0.07f
            val r = zone.radius
            mb.addQuad(
                zone.x - r, zy, zone.z + r,
                zone.x + r, zy, zone.z + r,
                zone.x + r, zy, zone.z - r,
                zone.x - r, zy, zone.z - r,
                0f, 1f, 0f, 0.21f, 0.23f, 0.26f
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

    fun buildStaticEnvironmentMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()

        for (m in TruckWorld.mountains) {
            val my = TruckWorld.terrainHeightAt(m.x, m.z) - 2f
            mb.addCone(m.x, my, m.z, m.radius, m.height, 12, 0.32f, 0.35f, 0.39f)
            mb.addCone(m.x, my + m.height * 0.65f, m.z, m.radius * 0.36f, m.height * 0.37f, 12, 0.90f, 0.93f, 0.97f)
        }

        for (b in TruckWorld.buildings) {
            val baseY = TruckWorld.terrainHeightAt(b.x, b.z)
            mb.addBox(b.x, baseY + b.height * 0.5f, b.z, b.width, b.height, b.depth, b.colorR, b.colorG, b.colorB)
            mb.addBox(b.x, baseY + b.height + 0.4f, b.z, b.width + 0.6f, 0.8f, b.depth + 0.6f, 0.18f, 0.20f, 0.24f)
            if (b.isSkyscraper) {
                mb.addBox(b.x, baseY + b.height + 1.8f, b.z, b.width * 0.5f, 2.2f, b.depth * 0.5f, 0.24f, 0.27f, 0.32f)
                mb.addCylinderY(b.x, baseY + b.height + 2.8f, b.z, 0.22f, 6.5f, 6, 0.72f, 0.74f, 0.78f)
            }
        }

        for (zone in TruckWorld.zones) {
            val zy = TruckWorld.terrainHeightAt(zone.x, zone.z)
            when (zone.type) {
                ZoneType.GAS_STATION -> {
                    val cx = zone.x + 8f
                    val cz = zone.z - 8f
                    mb.addCylinderY(cx - 4.5f, zy, cz - 2f, 0.35f, 5.6f, 8, 0.85f, 0.85f, 0.88f)
                    mb.addCylinderY(cx + 4.5f, zy, cz - 2f, 0.35f, 5.6f, 8, 0.85f, 0.85f, 0.88f)
                    mb.addBox(cx, zy + 5.8f, cz, 13f, 0.9f, 7.5f, 0.96f, 0.45f, 0.05f)
                    mb.addBox(cx, zy + 5.25f, cz, 13.1f, 0.25f, 7.6f, 0.95f, 0.95f, 0.98f)
                    // Twin fuel pumps
                    mb.addBox(cx - 2f, zy + 1.15f, cz - 2f, 1.3f, 2.3f, 0.85f, 0.92f, 0.94f, 0.96f)
                    mb.addBox(cx + 2f, zy + 1.15f, cz - 2f, 1.3f, 2.3f, 0.85f, 0.92f, 0.94f, 0.96f)
                    mb.addBox(cx - 2f, zy + 1.55f, cz - 2f, 1.34f, 0.55f, 0.88f, 0.12f, 0.65f, 0.38f)
                    mb.addBox(cx + 2f, zy + 1.55f, cz - 2f, 1.34f, 0.55f, 0.88f, 0.95f, 0.45f, 0.05f)
                }
                ZoneType.WORKSHOP -> {
                    val wx = zone.x + 10f
                    val wz = zone.z + 10f
                    mb.addBox(wx, zy + 3.2f, wz, 12.5f, 6.4f, 10.5f, 0.24f, 0.32f, 0.44f)
                    mb.addBox(wx, zy + 6.6f, wz, 13.1f, 0.6f, 11.1f, 0.14f, 0.56f, 0.88f)
                    mb.addBox(wx, zy + 2.2f, wz - 5.3f, 6.2f, 4.4f, 0.2f, 0.16f, 0.18f, 0.22f)
                }
                ZoneType.CARGO_HUB -> {
                    val hx = zone.x + 14f
                    val hz = zone.z + 12f
                    mb.addBox(hx, zy + 4.0f, hz, 16f, 8.0f, 12f, 0.34f, 0.38f, 0.44f)
                    mb.addBox(hx - 4f, zy + 1.8f, hz - 6.2f, 5.5f, 3.6f, 0.4f, 0.15f, 0.68f, 0.42f)
                    mb.addBox(hx + 3.5f, zy + 1.8f, hz - 6.2f, 5.5f, 3.6f, 0.4f, 0.22f, 0.26f, 0.30f)
                }
                ZoneType.PARKING -> Unit
            }
        }

        for (t in TruckWorld.trees) {
            val ty = TruckWorld.terrainHeightAt(t.x, t.z)
            val s = t.scale
            mb.addCylinderY(t.x, ty, t.z, 0.40f * s, 3.2f * s, 7, 0.38f, 0.25f, 0.14f)
            if (t.isPine) {
                mb.addCone(t.x, ty + 2.6f * s, t.z, 2.5f * s, 5.2f * s, 8, 0.11f, 0.34f, 0.16f)
                mb.addCone(t.x, ty + 5.2f * s, t.z, 1.8f * s, 4.4f * s, 8, 0.14f, 0.40f, 0.19f)
            } else {
                mb.addEllipsoid(t.x, ty + 5.0f * s, t.z, 2.4f * s, 2.1f * s, 2.4f * s, 6, 8, 0.17f, 0.46f, 0.17f)
            }
        }

        for (d in -200..200 step 50) {
            if (d == 0) continue
            addStreetlightPole(mb, d.toFloat(), 11.5f)
            addStreetlightPole(mb, 11.5f, d.toFloat())
        }

        for (sign in TruckWorld.roadSigns) {
            val sy = TruckWorld.terrainHeightAt(sign.x, sign.z)
            mb.addCylinderY(sign.x, sy, sign.z, 0.14f, 3.6f, 6, 0.72f, 0.74f, 0.78f)
            val (sr, sg, sb) = when (sign.type) {
                com.example.sim.SignType.STOP -> Triple(0.88f, 0.12f, 0.12f)
                com.example.sim.SignType.FUEL_AHEAD -> Triple(0.12f, 0.48f, 0.88f)
                com.example.sim.SignType.CURVE -> Triple(0.95f, 0.72f, 0.08f)
                else -> Triple(0.95f, 0.95f, 0.95f)
            }
            mb.addBox(sign.x, sy + 3.5f, sign.z, 1.2f, 1.2f, 0.18f, sr, sg, sb)
        }

        return mb.build()
    }

    private fun addStreetlightPole(mb: DynamicMeshBuilder, x: Float, z: Float) {
        val y = TruckWorld.terrainHeightAt(x, z)
        mb.addCylinderY(x, y, z, 0.16f, 7.2f, 6, 0.42f, 0.45f, 0.50f)
        mb.addBox(x, y + 7.2f, z - 0.8f, 0.35f, 0.25f, 1.8f, 0.48f, 0.50f, 0.55f)
    }

    fun buildNightWindowsAndLightsMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        for ((idx, b) in TruckWorld.buildings.withIndex()) {
            if (idx % 2 == 0) {
                val baseY = TruckWorld.terrainHeightAt(b.x, b.z)
                val rows = (b.height / 6f).toInt().coerceIn(1, 7)
                for (r in 1..rows) {
                    val wy = baseY + r * 5.2f
                    mb.addBox(b.x, wy, b.z, b.width + 0.12f, 0.9f, b.depth + 0.12f, 1.0f, 0.88f, 0.55f)
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

    fun buildCloudsMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        var seed = 4242
        fun rand(): Float {
            seed = (seed * 1103515245 + 12345) and 0x7fffffff
            return (seed % 10000) / 10000f
        }
        for (i in 0 until 28) {
            val cx = (rand() * 2f - 1f) * 1100f
            val cz = (rand() * 2f - 1f) * 1100f
            val cy = 145f + rand() * 45f
            val sx = 45f + rand() * 55f
            val sz = 32f + rand() * 42f
            mb.addEllipsoid(cx, cy, cz, sx * 0.55f, 9f, sz * 0.55f, 5, 8, 0.96f, 0.97f, 0.99f, 0.92f)
            mb.addEllipsoid(cx + sx * 0.25f, cy + 5f, cz + 4f, sx * 0.36f, 8f, sz * 0.42f, 5, 8, 0.98f, 0.98f, 1.0f, 0.92f)
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
