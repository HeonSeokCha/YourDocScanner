package com.chs.yourdocscanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.chs.yourdocscanner.crop.CropScreenRoot
import com.chs.yourdocscanner.crop.CropViewModel
import com.chs.yourdocscanner.permission.PermissionScreen
import com.chs.yourdocscanner.result.ScanResultScreenRoot
import com.chs.yourdocscanner.result.ScanResultViewModel
import com.chs.yourdocscanner.scan.DocumentScanScreenRoot
import com.chs.yourdocscanner.scan.DocumentScannerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun MainNavDisplay(
    modifier: Modifier = Modifier,
    backStack: NavBackStack<NavKey>,
) {
    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<YourDocScannerScreens.PermissionScreen> {
                PermissionScreen {
                    backStack.removeLastOrNull()
                    backStack.add(YourDocScannerScreens.DocumentScannerScreen)
                }
            }

            entry<YourDocScannerScreens.DocumentScannerScreen> {
                val viewModel: DocumentScannerViewModel = koinViewModel()
                DocumentScanScreenRoot(
                    viewModel = viewModel,
                    onNavigateCrop = {
                        backStack.add(YourDocScannerScreens.CropScreen(it))
                    },
                    onNavigateResult = { origin, crop, quad ->
                        backStack.add(YourDocScannerScreens.ScanResultScreen(origin, crop, quad))
                    }
                )
            }

            entry<YourDocScannerScreens.CropScreen> { key ->
                val viewModel: CropViewModel = koinViewModel<CropViewModel> {
                    parametersOf(
                        key.filePath,
                        key.detectQuad
                    )
                }
                CropScreenRoot(
                    viewModel = viewModel,
                    onNavigateResult = { crop, quad ->
                        backStack.removeLastOrNull()
                        backStack.add(YourDocScannerScreens.ScanResultScreen(key.filePath, crop, quad))
                    }
                )
            }

            entry<YourDocScannerScreens.ScanResultScreen> { key ->
                val viewModel: ScanResultViewModel = koinViewModel<ScanResultViewModel> {
                    parametersOf(
                        key.cropFilePath,
                        key.originFilePath,
                        key.detectQuad
                    )
                }

                ScanResultScreenRoot(
                    viewModel = viewModel,
                    onNavigateCrop = { filePath, detectQuad ->
                        backStack.removeLastOrNull()
                        backStack.add(YourDocScannerScreens.CropScreen(filePath, detectQuad))
                    },
                    onNavigateScan = {
                        backStack.clear()
                        backStack.add(YourDocScannerScreens.DocumentScannerScreen)
                    }
                )
            }
        }
    )
}