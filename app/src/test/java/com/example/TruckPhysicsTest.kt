package com.example

import com.example.model.CareerProfile
import com.example.sim.DrivingInputs
import com.example.sim.TrafficManager
import com.example.sim.TruckPhysicsEngine
import com.example.sim.TruckWorld
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TruckPhysicsTest {

    @Test
    fun truckAcceleratesSteersReversesAndConsumesFuel() {
        val physics = TruckPhysicsEngine()
        val traffic = TrafficManager()
        physics.applyCareerProfile(CareerProfile())
        physics.resetForNewRun(startX = -45f, startZ = 0f, startYaw = Math.PI.toFloat() / 2f)

        val initialX = physics.x
        val initialFuel = physics.fuelLiters

        // Accelerate forward for 3 seconds (180 frames at 1/60s)
        var lastTel = physics.update(0.016f, DrivingInputs(throttle = 1f), traffic, null, false)
        for (i in 0 until 180) {
            lastTel = physics.update(0.016f, DrivingInputs(throttle = 1f), traffic, null, false)
        }
        assertTrue("Truck should accelerate above 25 km/h, was ${lastTel.speedKmh}", lastTel.speedKmh > 25f)
        assertTrue("Truck should move east (+X)", physics.x > initialX + 10f)
        assertTrue("Fuel should be consumed", physics.fuelLiters < initialFuel)

        // Steer right while moving
        val yawBefore = physics.yawRad
        for (i in 0 until 45) {
            lastTel = physics.update(0.016f, DrivingInputs(throttle = 0.6f, steering = 0.8f), traffic, null, false)
        }
        assertTrue("Steering should change truck heading yaw", physics.yawRad != yawBefore)

        // Brake to stop
        for (i in 0 until 120) {
            lastTel = physics.update(0.016f, DrivingInputs(brake = 1f), traffic, null, false)
        }
        assertTrue("Braking should slow truck near zero, was ${lastTel.speedKmh}", lastTel.speedKmh < 3f)

        // Shift to reverse and move backward
        physics.toggleReverseAuto()
        assertEquals(-1, physics.gear)
        for (i in 0 until 60) {
            lastTel = physics.update(0.016f, DrivingInputs(throttle = 1f), traffic, null, false)
        }
        assertTrue("Truck should move backward in reverse gear", physics.speedMs < -1.5f)
    }

    @Test
    fun articulatedTrailerFollowsAndWorldFacilitiesExist() {
        val physics = TruckPhysicsEngine()
        val traffic = TrafficManager()
        physics.resetForNewRun(-45f, 0f, Math.PI.toFloat() / 2f)
        physics.attachTrailer("#2563EB")
        assertTrue(physics.hasTrailer)

        for (i in 0 until 60) {
            physics.update(0.016f, DrivingInputs(throttle = 1f, steering = 0.4f), traffic, null, false)
        }
        assertTrue(physics.trailerX < physics.x + 15f)

        assertTrue(TruckWorld.queryRoad(0f, 0f).onRoad)
        assertFalse(TruckWorld.queryRoad(500f, 500f).onRoad)
        assertTrue(TruckWorld.terrainHeightAt(0f, 760f) > TruckWorld.terrainHeightAt(0f, 0f))
        assertTrue(traffic.vehicles.isNotEmpty())
    }
}
