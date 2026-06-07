package dev.upaya.autohrv.ui.hr

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.upaya.autohrv.R
import dev.upaya.autohrv.domain.bluetooth.GetBluetoothPermissionUseCase
import dev.upaya.autohrv.ui.breathing.BreathingPacerViewModel
import dev.upaya.autohrv.ui.game.BreathBirdScreen
import dev.upaya.autohrv.ui.game.BreathBirdViewModel
import dev.upaya.autohrv.ui.theme.AutoHrvBg
import dev.upaya.autohrv.ui.theme.AutoHrvAccent
import dev.upaya.autohrv.ui.theme.AutoHrvMuted
import dev.upaya.autohrv.ui.theme.AutoHrvTheme
import dagger.hilt.android.AndroidEntryPoint

private enum class AppTab { HRV, BIRD }

@AndroidEntryPoint
class HRActivity : ComponentActivity() {

    private val hrViewModel:        HrvViewModel           by viewModels()
    private val breathingViewModel: BreathingPacerViewModel by viewModels()
    private val birdViewModel:      BreathBirdViewModel    by viewModels()

    private val getBluetoothPermissions = GetBluetoothPermissionUseCase(
        activity  = this,
        onGranted = { hrViewModel.connect() },
        onDenied  = {
            Toast.makeText(applicationContext, "Needed Bluetooth permissions are missing", Toast.LENGTH_LONG).show()
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoHrvTheme {
                AppNavigation(
                    hrViewModel        = hrViewModel,
                    breathingViewModel = breathingViewModel,
                    birdViewModel      = birdViewModel,
                )
            }
        }
        getBluetoothPermissions()
    }
}

@Composable
private fun AppNavigation(
    hrViewModel:        HrvViewModel,
    breathingViewModel: BreathingPacerViewModel,
    birdViewModel:      BreathBirdViewModel,
) {
    var activeTab by rememberSaveable { mutableStateOf(AppTab.HRV) }

    Scaffold(
        containerColor = AutoHrvBg,
        bottomBar = {
            if (activeTab == AppTab.HRV) {
                AppBottomBar(activeTab = activeTab, onTabSelected = { activeTab = it })
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (activeTab) {
                AppTab.HRV  -> HRScreen(hrViewModel, breathingViewModel)
                AppTab.BIRD -> {
                    // Full-bleed: bird screen manages its own padding
                    Box(modifier = Modifier.fillMaxSize()) {
                        BreathBirdScreen(birdViewModel)
                        // overlay the bottom bar so the user can navigate back
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppBottomBar(
                                activeTab   = activeTab,
                                onTabSelected = { activeTab = it },
                                modifier    = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    activeTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier        = modifier,
        containerColor  = AutoHrvBg,
        contentColor    = AutoHrvMuted,
        tonalElevation  = 0.dp,
    ) {
        NavigationBarItem(
            selected  = activeTab == AppTab.HRV,
            onClick   = { onTabSelected(AppTab.HRV) },
            icon      = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            label     = { Text("HRV") },
            colors    = navBarItemColors(),
        )
        NavigationBarItem(
            selected  = activeTab == AppTab.BIRD,
            onClick   = { onTabSelected(AppTab.BIRD) },
            icon      = { Icon(painterResource(R.drawable.ic_bird), contentDescription = null) },
            label     = { Text("Breath Bird") },
            colors    = navBarItemColors(),
        )
    }
}

@Composable
private fun navBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor       = AutoHrvAccent,
    selectedTextColor       = AutoHrvAccent,
    unselectedIconColor     = AutoHrvMuted,
    unselectedTextColor     = AutoHrvMuted,
    indicatorColor          = AutoHrvAccent.copy(alpha = 0.12f),
)
