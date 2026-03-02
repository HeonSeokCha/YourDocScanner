package com.chs.yourdocscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.chs.yourdocscanner.ui.theme.YourDocScannerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
            val backStack: NavBackStack<NavKey> = rememberNavBackStack().apply {
                if (cameraPermissionState.status.isGranted) {
                    this.add(YourDocScannerScreens.DocumentScannerScreen)
                } else {
                    this.add(YourDocScannerScreens.PermissionScreen)
                }
            }

            YourDocScannerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainNavDisplay(
                        backStack = backStack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}