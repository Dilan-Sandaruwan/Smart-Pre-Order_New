package com.smartpreorder.app.ui.customer

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.smartpreorder.app.ui.Ic

@Composable
fun CustomerBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    activeOrderCount: Int
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Ic.Restaurant, null) },
            label = { Text("Restaurants") }
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                BadgedBox(badge = { if (activeOrderCount > 0) Badge { Text("$activeOrderCount") } }) {
                    Icon(Ic.Receipt, null)
                }
            },
            label = { Text("My orders") }
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Ic.Person, null) },
            label = { Text("Profile") }
        )
    }
}
