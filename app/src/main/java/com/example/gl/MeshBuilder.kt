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
    private val data = ArrayList<Float>(4096)

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

    fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        // +Y top
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, r, g, b, a)
        // -Y bottom
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r * 0.7f, g * 0.7f, b * 0.7f, a)
        // +Z front
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r * 0.95f, g * 0.95f, b * 0.95f, a)
        // -Z back
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r * 0.88f, g * 0.88f, b * 0.88f, a)
        // +X right
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r * 0.90f, g * 0.90f, b * 0.90f, a)
        // -X left
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r * 0.90f, g * 0.90f, b * 0.90f, a)
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
        val topY = baseY + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            val x0 = cx + c0 * radius; val z0 = cz + s0 * radius
            val x1 = cx + c1 * radius; val z1 = cz + s1 * radius
            addVertex(x0, baseY, z0, c0, 0f, s0, r, g, b)
            addVertex(x1, baseY, z1, c1, 0f, s1, r, g, b)
            addVertex(x1, topY, z1, c1, 0f, s1, r, g, b)

            addVertex(x0, baseY, z0, c0, 0f, s0, r, g, b)
            addVertex(x1, topY, z1, c1, 0f, s1, r, g, b)
            addVertex(x0, topY, z0, c0, 0f, s0, r, g, b)
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

    fun buildUnitWheel(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        // Tire cylinder along X axis (width 0.38, radius 0.52)
        val segs = 16
        val hw = 0.22f
        val rad = 0.52f
        for (i in 0 until segs) {
            val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
            val y0 = cos(a0) * rad; val z0 = sin(a0) * rad
            val y1 = cos(a1) * rad; val z1 = sin(a1) * rad
            val ny = cos((a0 + a1) * 0.5f)
            val nz = sin((a0 + a1) * 0.5f)
            // Tread
            mb.addQuad(-hw, y0, z0, hw, y0, z0, hw, y1, z1, -hw, y1, z1, 0f, ny, nz, 0.14f, 0.15f, 0.17f)
            // Sidewalls / Rim
            val rimR = rad * 0.56f
            val ry0 = cos(a0) * rimR; val rz0 = sin(a0) * rimR
            val ry1 = cos(a1) * rimR; val rz1 = sin(a1) * rimR
            mb.addQuad(hw, 0f, 0f, hw, ry0, rz0, hw, ry1, rz1, hw, 0f, 0f, 1f, 0f, 0f, 0.72f, 0.76f, 0.80f)
            mb.addQuad(-hw, 0f, 0f, -hw, ry1, rz1, -hw, ry0, rz0, -hw, 0f, 0f, -1f, 0f, 0f, 0.72f, 0.76f, 0.80f)
            mb.addQuad(hw, ry0, rz0, hw, y0, z0, hw, y1, z1, hw, ry1, rz1, 1f, 0f, 0f, 0.16f, 0.17f, 0.19f)
            mb.addQuad(-hw, ry1, rz1, -hw, y1, z1, -hw, y0, z0, -hw, ry0, rz0, -1f, 0f, 0f, 0.16f, 0.17f, 0.19f)
        }
        return mb.build()
    }

    fun buildTerrainMesh(): MeshBuffer {
        val mb = DynamicMeshBuilder()
        val grid = 48
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

                // Color zones: Coastal ocean water east of Puerto Bahia, urban concrete in central, green meadows elsewhere
                val isOcean = cx > 870f && cz < -180f
                val isUrban = hypot(cx, cz) < 280f
                val isSierra = cz > 450f

                val (r, g, b) = when {
                    isOcean -> Triple(0.10f, 0.34f, 0.56f)
                    isUrban -> Triple(0.28f, 0.30f, 0.31f)
                    isSierra -> Triple(0.26f, 0.38f, 0.22f)
                    (gx + gz) % 3 == 0 -> Triple(0.20f, 0.44f, 0.18f)
                    else -> Triple(0.24f, 0.48f, 0.20f)
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
        // Paved road ribbons subdivided every 24m so hills follow terrain smoothly
        for (road in TruckWorld.roads) {
            val steps = (road.length / 24f).toInt().coerceAtLeast(2)
            val rightX = -road.dirZ
            val rightZ = road.dirX
            val halfW = road.width * 0.5f
            val shoulderW = halfW + 1.4f

            for (i in 0 until steps) {
                val t0 = i.toFloat() / steps
                val t1 = (i + 1).toFloat() / steps
                val cx0 = road.x1 + (road.x2 - road.x1) * t0
                val cz0 = road.z1 + (road.z2 - road.z1) * t0
                val cx1 = road.x1 + (road.x2 - road.x1) * t1
                val cz1 = road.z1 + (road.z2 - road.z1) * t1

                val y0 = TruckWorld.terrainHeightAt(cx0, cz0) + 0.06f
                val y1 = TruckWorld.terrainHeightAt(cx1, cz1) + 0.06f

                // Concrete/gravel shoulder
                mb.addQuad(
                    cx0 - rightX * shoulderW, y0 - 0.02f, cz0 - rightZ * shoulderW,
                    cx0 + rightX * shoulderW, y0 - 0.02f, cz0 + rightZ * shoulderW,
                    cx1 + rightX * shoulderW, y1 - 0.02f, cz1 + rightZ * shoulderW,
                    cx1 - rightX * shoulderW, y1 - 0.02f, cz1 - rightZ * shoulderW,
                    0f, 1f, 0f, 0.36f, 0.37f, 0.38f
                )

                // Textured-look asphalt strip (alternating subtle tone for longitudinal speed perception)
                val shade = if (i % 2 == 0) 0.16f else 0.18f
                mb.addQuad(
                    cx0 - rightX * halfW, y0, cz0 - rightZ * halfW,
                    cx0 + rightX * halfW, y0, cz0 + rightZ * halfW,
                    cx1 + rightX * halfW, y1, cz1 + rightZ * halfW,
                    cx1 - rightX * halfW, y1, cz1 - rightZ * halfW,
                    0f, 1f, 0f, shade, shade + 0.01f, shade + 0.02f
                )

                // White outer edge lines
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

                // Dashed center line (yellow on highways, white on secondary roads)
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
            }
        }

        // Paved tarmac pads for Hubs, Gas Stations, Workshops, Parking
        for (zone in TruckWorld.zones) {
            val zy = TruckWorld.terrainHeightAt(zone.x, zone.z) + 0.07f
            val r = zone.radius
            mb.addQuad(
                zone.x - r, zy, zone.z + r,
                zone.x + r, zy, zone.z + r,
                zone.x + r, zy, zone.z - r,
                zone.x - r, zy, zone.z - r,
                0f, 1f, 0f, 0.22f, 0.24f, 0.27f
            )
            // Colored border frame indicating facility type
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

        // Mountains
        for (m in TruckWorld.mountains) {
            val my = TruckWorld.terrainHeightAt(m.x, m.z) - 2f
            mb.addCone(m.x, my, m.z, m.radius, m.height, 10, 0.34f, 0.37f, 0.40f)
            // Snow cap
            mb.addCone(m.x, my + m.height * 0.65f, m.z, m.radius * 0.36f, m.height * 0.37f, 10, 0.90f, 0.93f, 0.97f)
        }

        // Buildings
        for (b in TruckWorld.buildings) {
            val baseY = TruckWorld.terrainHeightAt(b.x, b.z)
            mb.addBox(b.x, baseY + b.height * 0.5f, b.z, b.width, b.height, b.depth, b.colorR, b.colorG, b.colorB)
            // Roof lip
            mb.addBox(b.x, baseY + b.height + 0.4f, b.z, b.width + 0.6f, 0.8f, b.depth + 0.6f, 0.18f, 0.20f, 0.24f)
        }

        // Gas station canopies & Workshop structures
        for (zone in TruckWorld.zones) {
            val zy = TruckWorld.terrainHeightAt(zone.x, zone.z)
            when (zone.type) {
                ZoneType.GAS_STATION -> {
                    val cx = zone.x + 8f
                    val cz = zone.z - 8f
                    mb.addCylinderY(cx - 4f, zy, cz - 2f, 0.35f, 5.5f, 6, 0.85f, 0.85f, 0.88f)
                    mb.addCylinderY(cx + 4f, zy, cz - 2f, 0.35f, 5.5f, 6, 0.85f, 0.85f, 0.88f)
                    mb.addBox(cx, zy + 5.7f, cz, 12f, 0.9f, 7f, 0.96f, 0.45f, 0.05f)
                    mb.addBox(cx, zy + 1.1f, cz - 2f, 1.4f, 2.2f, 0.9f, 0.95f, 0.95f, 0.95f)
                }
                ZoneType.WORKSHOP -> {
                    val wx = zone.x + 10f
                    val wz = zone.z + 10f
                    mb.addBox(wx, zy + 3.2f, wz, 12f, 6.4f, 10f, 0.24f, 0.32f, 0.44f)
                    mb.addBox(wx, zy + 6.6f, wz, 12.6f, 0.6f, 10.6f, 0.14f, 0.56f, 0.88f)
                }
                ZoneType.CARGO_HUB -> {
                    val hx = zone.x + 14f
                    val hz = zone.z + 12f
                    mb.addBox(hx, zy + 4.0f, hz, 16f, 8.0f, 12f, 0.34f, 0.38f, 0.44f)
                    mb.addBox(hx - 4f, zy + 1.6f, hz - 6.2f, 5.5f, 3.2f, 0.4f, 0.15f, 0.68f, 0.42f)
                }
                ZoneType.PARKING -> Unit
            }
        }

        // Trees
        for (t in TruckWorld.trees) {
            val ty = TruckWorld.terrainHeightAt(t.x, t.z)
            val s = t.scale
            mb.addCylinderY(t.x, ty, t.z, 0.42f * s, 3.2f * s, 6, 0.38f, 0.25f, 0.14f)
            if (t.isPine) {
                mb.addCone(t.x, ty + 2.6f * s, t.z, 2.5f * s, 5.2f * s, 7, 0.11f, 0.34f, 0.16f)
                mb.addCone(t.x, ty + 5.2f * s, t.z, 1.8f * s, 4.4f * s, 7, 0.14f, 0.40f, 0.19f)
            } else {
                mb.addBox(t.x, ty + 4.8f * s, t.z, 3.8f * s, 3.4f * s, 3.8f * s, 0.18f, 0.46f, 0.18f)
            }
        }

        // Streetlights along Ciudad Central main avenues
        for (d in -200..200 step 50) {
            if (d == 0) continue
            addStreetlightPole(mb, d.toFloat(), 11.5f)
            addStreetlightPole(mb, 11.5f, d.toFloat())
        }

        // Road signs
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
        // Glowing building window strips
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
        // Streetlight bulbs
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
            mb.addBox(cx, cy, cz, sx, 12f, sz, 0.96f, 0.97f, 0.99f, 0.92f)
            mb.addBox(cx + sx * 0.25f, cy + 7f, cz + 4f, sx * 0.6f, 10f, sz * 0.7f, 0.98f, 0.98f, 1.0f, 0.92f)
        }
        return mb.build()
    }

    fun buildScreenQuad(): ScreenQuadBuffer {
        // Interleaved: vec2 pos, vec2 uv -> 4 floats per vertex, 6 vertices
        val verts = floatArrayOf(
            0f, 0f, 0f, 0f,
            1f, 0f, 1f, 0f,
            1f, 1f, 1f, 1f,
            0f, 0f, 0f, 0f,
            1f, 1f, 1f, 1f,
            0f, 1f, 0f, 1f
        )
        // Custom VAO for 2+2 layout
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
