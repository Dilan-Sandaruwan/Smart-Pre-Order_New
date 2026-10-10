package com.smartpreorder.app.ui.restaurant

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.smartpreorder.app.ui.Ic

@Composable
fun RestaurantBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    pendingCount: Int
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                BadgedBox(badge = { if (pendingCount > 0) Badge { Text("$pendingCount") } }) {
                    Icon(Ic.Receipt, null)
                }
            },
            label = { Text("Orders") }
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Ic.Menu, null) },
            label = { Text("Menu") }
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Ic.Chart, null) },
            label = { Text("Sales") }
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Ic.Store, null) },
            label = { Text("Restaurant") }
        )
    }
}
