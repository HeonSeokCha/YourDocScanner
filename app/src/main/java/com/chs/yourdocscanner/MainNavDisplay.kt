package com.chs.yourdocscanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.chs.yourdocscanner.permission.PermissionScreen
import com.chs.yourdocscanner.scan.DocumentScanScreenRoot
import com.chs.yourdocscanner.scan.DocumentScannerViewModel
import org.koin.androidx.compose.koinViewModel

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
                DocumentScanScreenRoot(viewModel = viewModel)
            }
        }
    )
}