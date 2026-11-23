package com.example

import com.example.model.PlayerControlMode
import com.example.sim.DrivingInputs
import com.example.sim.TrafficManager
import com.example.sim.TruckPhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class TruckPhysicsDriverTest {

    @Test
    fun `driver can exit stopped truck walk on foot and re-enter near cab door`() {
        val engine = TruckPhysicsEngine()
        val traffic = TrafficManager()
        engine.resetForNewRun()

        // Initially in truck
        assertEquals(PlayerControlMode.IN_TRUCK, engine.controlMode)
        assertTrue(engine.exitTruck())
        assertEquals(PlayerControlMode.ON_FOOT, engine.controlMode)
        assertTrue(engine.isDriverNearCabDoor())

        val startX = engine.driverX
        val startZ = engine.driverZ

        // Walk forward with sprint for ~2 seconds
        repeat(60) {
            engine.update(
                dt = 0.033f,
                inputs = DrivingInputs(moveX = 0.4f, moveY = 1.0f, sprint = true),
                trafficManager = traffic,
                activeMission = null,
                missionLoaded = false
            )
        }

        val movedDist = hypot(engine.driverX - startX, engine.driverZ - startZ)
        assertTrue("Expected driver to walk/sprint > 4m, moved $movedDist", movedDist > 4.0f)
        assertTrue(engine.driverWalkPhase > 1.0f)

        // Cannot re-enter when far away from cab door
        assertFalse(engine.isDriverNearCabDoor())
        assertFalse(engine.enterTruck())
        assertEquals(PlayerControlMode.ON_FOOT, engine.controlMode)

        // Move back next to cab door and enter
        engine.driverX = engine.x
        engine.driverZ = engine.z - 2.0f
        assertTrue(engine.isDriverNearCabDoor())
        assertTrue(engine.enterTruck())
        assertEquals(PlayerControlMode.IN_TRUCK, engine.controlMode)
    }

    @Test
    fun `driver cannot exit truck while driving at high speed`() {
        val engine = TruckPhysicsEngine()
        engine.resetForNewRun()
        engine.speedMs = 15f // ~54 km/h

        assertFalse(engine.exitTruck())
        assertEquals(PlayerControlMode.IN_TRUCK, engine.controlMode)
    }
}
