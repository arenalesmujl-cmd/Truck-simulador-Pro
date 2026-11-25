package com.example.gl

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.CareerProfile
import com.example.model.MissionContract
import com.example.sim.DrivingInputs

@Composable
fun TruckGLSurfaceView(
    renderer: TruckGLRenderer,
    inputs: DrivingInputs,
    profile: CareerProfile,
    activeMission: MissionContract?,
    missionLoaded: Boolean,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(inputs) {
        renderer.currentInputs = inputs
    }
    LaunchedEffect(profile) {
        renderer.currentProfile = profile
        renderer.physics.applyCareerProfile(profile)
    }
    LaunchedEffect(activeMission, missionLoaded) {
        renderer.activeMission = activeMission
        renderer.missionLoaded = missionLoaded
    }

    DisposableEffect(renderer) {
        renderer.audioEngine.start()
        onDispose {
            renderer.audioEngine.stop()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            renderer.appContext = context.applicationContext
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer)
                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            }
        }
    )
}
