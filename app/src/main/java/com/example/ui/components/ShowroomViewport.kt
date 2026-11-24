package com.example.ui.components

import android.opengl.GLSurfaceView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gl.ShowroomGLRenderer
import com.example.model.TruckStyle
import com.example.ui.theme.SimOutline
import com.example.ui.theme.SimSurfaceGlass
import com.example.ui.theme.SkyCyan

@Composable
fun ShowroomViewport(
    truckStyle: TruckStyle,
    paintHex: String,
    showDriver: Boolean = true,
    badgeLabel: String? = "360° • DESLIZA PARA ROTAR",
    modifier: Modifier = Modifier
) {
    val renderer = remember { ShowroomGLRenderer() }
    var orbitOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(truckStyle, paintHex, showDriver, orbitOffset) {
        renderer.truckStyle = truckStyle
        renderer.paintHex = paintHex
        renderer.showDriver = showDriver
        renderer.userOrbitOffsetRad = orbitOffset
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    orbitOffset += dragAmount * 0.008f
                }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                GLSurfaceView(context).apply {
                    setEGLContextClientVersion(3)
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                }
            },
            onRelease = { view ->
                view.onPause()
            }
        )

        if (badgeLabel != null) {
            Surface(
                color = SimSurfaceGlass,
                border = BorderStroke(1.dp, SimOutline),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
