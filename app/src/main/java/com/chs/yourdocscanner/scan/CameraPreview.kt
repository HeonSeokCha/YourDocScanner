package com.chs.yourdocscanner.scan

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    surfaceRequest: SurfaceRequest?,
    onBindCamera: () -> Unit
) {
    LaunchedEffect(lifecycleOwner) {
        onBindCamera()
    }

    surfaceRequest?.let { request ->
        CameraXViewfinder(
            modifier = modifier,
            surfaceRequest = request,
        )
    }
}

@Composable
fun CameraBottomControls(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit = {},
    isAutoCapture: Boolean = true,
    onModeChange: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(75.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .padding(8.dp)
                    .background(Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable { onCaptureClick() }
            )
        }

        CaptureToggle(
            isAutoCapture = isAutoCapture,
            onModeChange = onModeChange
        )
    }
}

@Composable
fun CaptureToggle(
    isAutoCapture: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    val toggleColor = Color(0xFF3A3A3A)

    Box(
        modifier = Modifier
            .background(toggleColor, RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (!isAutoCapture) Color.White else Color.Transparent
                    )
                    .clickable { onModeChange(false) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "수동",
                    color = if (!isAutoCapture) Color.Black else Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isAutoCapture) Color.White else Color.Transparent
                    )
                    .clickable { onModeChange(true) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "자동 캡처",
                    color = if (isAutoCapture) Color.Black else Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}