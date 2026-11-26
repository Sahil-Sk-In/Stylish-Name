package com.sahil.namemaker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahil.namemaker.data.repository.AppRepository
import com.sahil.namemaker.ui.components.AppHeader
import com.sahil.namemaker.ui.theme.*

enum class AppTab(val title: String, val icon: ImageVector) {
    STUDIO("Studio", Icons.Default.AutoAwesome),
    SHOWCASE("VIP Names & Bios", Icons.Default.Diamond)
}

@Composable
fun MainScreen(repository: AppRepository) {
    var currentTab by remember { mutableStateOf(AppTab.STUDIO) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Name Maker",
                subtitle = "Made with ❤️ by Sahil"
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkBg,
                contentColor = TextPrimary,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (selected) ElectricCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                color = if (selected) ElectricCyan else TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = SurfaceElevated
                        )
                    )
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
        ) {
            when (currentTab) {
                AppTab.STUDIO -> StudioScreen(repository = repository)
                AppTab.SHOWCASE -> ShowcaseScreen(repository = repository)
            }
        }
    }
}
