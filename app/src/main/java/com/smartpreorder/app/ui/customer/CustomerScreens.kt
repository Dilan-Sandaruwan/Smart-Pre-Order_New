@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.customer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartpreorder.app.data.MenuItem
import com.smartpreorder.app.data.Order
import com.smartpreorder.app.data.Profile
import com.smartpreorder.app.data.Restaurant
import com.smartpreorder.app.data.RestaurantStat
import com.smartpreorder.app.data.Review
import com.smartpreorder.app.ui.ACTIVE_STATUSES
import com.smartpreorder.app.ui.AuthViewModel
import com.smartpreorder.app.ui.ConfirmDialog
import com.smartpreorder.app.ui.CustomerViewModel
import com.smartpreorder.app.ui.EmptyState
import com.smartpreorder.app.ui.Ic
import com.smartpreorder.app.ui.Notices
import com.smartpreorder.app.ui.RatingBar
import com.smartpreorder.app.ui.Section
import com.smartpreorder.app.ui.SmallTag
import com.smartpreorder.app.ui.StatusChip
import com.smartpreorder.app.ui.StatusStepper
import com.smartpreorder.app.ui.formatTime
import com.smartpreorder.app.ui.money
import com.smartpreorder.app.ui.orderTypeLabel
import com.smartpreorder.app.ui.sanitizeImageUrl
import com.smartpreorder.app.ui.shortId

// =====================================================================
//  Navigation host for the customer role
// =====================================================================
@Composable
fun CustomerApp(auth: AuthViewModel, profile: Profile) {
    val vm: CustomerViewModel = viewModel(key = "customer-${profile.id}")
    LaunchedEffect(vm) { vm.start() }
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "tabs") {
        composable("tabs") { CustomerTabs(vm, auth, nav) }
        composable("restaurant/{id}") { entry ->
            RestaurantMenuScreen(vm, entry.arguments?.getString("id").orEmpty(), nav)
        }
        composable("cart") { CartScreen(vm, nav) }
        composable("order/{id}") { entry ->
            OrderDetailScreen(vm, auth, entry.arguments?.getString("id").orEmpty(), nav)
        }
    }
}

@Composable
private fun CustomerTabs(vm: CustomerViewModel, auth: AuthViewModel, nav: NavHostController) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    Notices(auth, snack)

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            CustomerBottomBar(
                selectedTab = tab,
                onTabSelected = { tab = it },
                activeOrderCount = vm.activeOrderCount
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> RestaurantsTab(vm, nav)
                1 -> OrdersTab(vm, nav)
                else -> CustomerProfileTab(vm, auth)
            }
        }
    }
}



// =====================================================================
//  Tab 1: restaurants + food search
// =====================================================================
data class PopularCategoryItem(
    val name: String,
    val emoji: String,
    val bgHex: Long = 0xFFFFF3E0
)

val POPULAR_CATEGORIES = listOf(
    PopularCategoryItem("Rice & Curry", "🍛", 0xFFFFF3E0),
    PopularCategoryItem("Burgers", "🍔", 0xFFFFEBEE),
    PopularCategoryItem("Shawarma", "🌯", 0xFFF3E5F5),
    PopularCategoryItem("Desserts", "🧁", 0xFFFCE4EC),
    PopularCategoryItem("Beverages", "🥤", 0xFFE0F7FA),
    PopularCategoryItem("Cafe", "☕", 0xFFEFEBE9),
    PopularCategoryItem("Pasta", "🍝", 0xFFFFF8E1),
    PopularCategoryItem("Juices", "🧃", 0xFFF1F8E9),
    PopularCategoryItem("Bakery", "🥐", 0xFFFFF3E0),
    PopularCategoryItem("Chinese", "🥢", 0xFFFFEBEE),
    PopularCategoryItem("Indian", "🥘", 0xFFF3E5F5),
    PopularCategoryItem("Kottu", "🍲", 0xFFFFE0B2),
    PopularCategoryItem("Pizza", "🍕", 0xFFFFEBEE),
    PopularCategoryItem("Short Eats", "🥟", 0xFFFFF8E1),
    PopularCategoryItem("Fast Food", "🍟", 0xFFFFEBEE),
    PopularCategoryItem("Vegetarian", "🥗", 0xFFE8F5E9),
    PopularCategoryItem("Wraps", "🌯", 0xFFF3E5F5),
    PopularCategoryItem("Sea Food", "🦐", 0xFFE0F7FA)
)

data class OfferBanner(
    val id: String,
    val title: String,
    val discountText: String,
    val subtext: String,
    val startColor: Color,
    val endColor: Color
)

val DEFAULT_OFFERS = listOf(
    OfferBanner(
        id = "1",
        title = "Big Cravings, Bigger Savings",
        discountText = "Rs. 1000 OFF",
        subtext = "On orders of Rs. 3,000+",
        startColor = Color(0xFF900C3F),
        endColor = Color(0xFFC70039)
    ),
    OfferBanner(
        id = "2",
        title = "Special Discount",
        discountText = "20% OFF",
        subtext = "On all Kottu & Burgers today",
        startColor = Color(0xFF1E5128),
        endColor = Color(0xFF4E9F3D)
    ),
    OfferBanner(
        id = "3",
        title = "Super Deal",
        discountText = "Free Delivery",
        subtext = "For your first 3 pre-orders",
        startColor = Color(0xFF1F4287),
        endColor = Color(0xFF278EA5)
    )
)

@Composable
private fun RestaurantsTab(vm: CustomerViewModel, nav: NavHostController) {
    LaunchedEffect(Unit) { vm.refreshRestaurants() }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val q = vm.query.trim()
    val cat = selectedCategory

    val list = vm.restaurants.filter { r ->
        val matchesQuery = q.isEmpty() || r.name.contains(q, true) || (r.address ?: "").contains(q, true) || (r.description ?: "").contains(q, true)
        val matchesCategory = cat == null || r.name.contains(cat, true) || (r.description ?: "").contains(cat, true)
        matchesQuery && matchesCategory
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📍", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Work", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Select location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }

                OutlinedTextField(
                    value = vm.query,
                    onValueChange = vm::onQuery,
                    placeholder = { Text("Find something delicious 🤤") },
                    leadingIcon = { Icon(Ic.Search, contentDescription = null) },
                    trailingIcon = {
                        if (vm.query.isNotEmpty()) {
                            IconButton(onClick = { vm.onQuery("") }) { Icon(Ic.Close, null) }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DEFAULT_OFFERS, key = { it.id }) { offer ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .width(320.dp)
                            .height(160.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(offer.startColor, offer.endColor)))
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        offer.title.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFFFFD700),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            offer.discountText,
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.Black,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    offer.subtext,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Popular Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedCategory != null) {
                        TextButton(onClick = { selectedCategory = null }) {
                            Text("Show All")
                        }
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(POPULAR_CATEGORIES, key = { it.name }) { catItem ->
                        val isSelected = selectedCategory == catItem.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedCategory = if (isSelected) null else catItem.name
                            }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(catItem.bgHex),
                                shadowElevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(catItem.emoji, style = MaterialTheme.typography.headlineMedium)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                catItem.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (vm.dishResults.isNotEmpty()) {
            item { Section("Dishes") }
            items(vm.dishResults, key = { "dish-" + it.id }) { d ->
                Card(onClick = { nav.navigate("restaurant/${d.restaurantId}") }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (!d.imageUrl.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = d.imageUrl,
                                    contentDescription = d.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text(d.restaurants?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(money(d.price), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                if (selectedCategory != null) "Restaurants for '$selectedCategory'" else "Restaurants",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (list.isEmpty()) {
            item { EmptyState(if (vm.loadingRestaurants) "Loading..." else "No restaurants found") }
        }

        items(list, key = { it.id }) { r ->
            RestaurantCard(r, vm.stats[r.id]) { nav.navigate("restaurant/${r.id}") }
        }
    }
}

@Composable
private fun RestaurantCard(r: Restaurant, stat: RestaurantStat?, onClick: () -> Unit) {
    val context = LocalContext.current
    val coverUrl = sanitizeImageUrl(r.coverImageUrl)
    val logoUrl = sanitizeImageUrl(r.imageUrl)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF800000), Color(0xFFD32F2F)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽️ ${r.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
                            .listener(onError = { _, result ->
                                Log.e("RestaurantCard", "Error loading coverUrl: $coverUrl", result.throwable)
                            })
                            .build(),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    color = if (r.isOpen) Color(0xFFD4F4DD) else Color(0xFFFFD6D6),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                ) {
                    Text(
                        if (r.isOpen) "Open" else "Closed",
                        color = if (r.isOpen) Color(0xFF14692B) else Color(0xFF8F1111),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp)
                        .offset(y = 20.dp)
                        .size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            r.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!logoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(logoUrl)
                                    .crossfade(true)
                                    .listener(onError = { _, result ->
                                        Log.e("RestaurantCard", "Error loading logoUrl: $logoUrl", result.throwable)
                                    })
                                    .build(),
                                contentDescription = "Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        r.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (stat != null && stat.reviewCount > 0) {
                        Surface(
                            color = Color(0xFFFFF3CD),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Ic.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                Text(" ${stat.avgRating}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (!r.description.isNullOrBlank()) {
                    Text(r.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 2)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    r.address?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                    r.estimatedPrepTimeMinutes?.let {
                        Text("⏱️ $it mins", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// =====================================================================
//  Restaurant menu
// =====================================================================
@Composable
private fun RestaurantMenuScreen(vm: CustomerViewModel, id: String, nav: NavHostController) {
    val r = vm.restaurants.firstOrNull { it.id == id }
    LaunchedEffect(id) { vm.loadMenu(id) }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    var selectedCat by remember { mutableStateOf<String?>(null) }
    var showReport by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }
    val stat = vm.stats[id]

    val cats = vm.categories.filter { c -> vm.menu.any { it.categoryId == c.id } }
    val shown = vm.menu.filter { selectedCat == null || it.categoryId == selectedCat }
    val canOrder = r?.isOpen == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(r?.name ?: "Menu") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } },
                actions = {
                    IconButton(onClick = { nav.navigate("cart") }) {
                        BadgedBox(badge = { if (vm.cartCount > 0) Badge { Text("${vm.cartCount}") } }) {
                            Icon(Ic.Cart, "Cart")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (vm.cartCount > 0 && vm.cartRestaurantId == id) {
                Surface(tonalElevation = 3.dp) {
                    Button(onClick = { nav.navigate("cart") }, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("View cart  •  ${vm.cartCount} items  •  ${money(vm.cartTotal)}")
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if (r != null) {
                    val coverUrl = sanitizeImageUrl(r.coverImageUrl)
                    val logoUrl = sanitizeImageUrl(r.imageUrl)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                            ) {
                                if (!coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = "Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.horizontalGradient(listOf(Color(0xFF800000), Color(0xFFD32F2F)))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "🍽️ ${r.name}",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 14.dp)
                                        .offset(y = 20.dp)
                                        .size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!logoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = logoUrl,
                                                contentDescription = "Logo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                                            )
                                        } else {
                                            Text(
                                                r.name.take(1).uppercase(),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(r.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                if (!r.description.isNullOrBlank()) Text(r.description, style = MaterialTheme.typography.bodyMedium)
                                if (!r.address.isNullOrBlank()) Text("📍 ${r.address}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                if (!r.phone.isNullOrBlank()) Text("📞 ${r.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (stat != null && stat.reviewCount > 0) {
                                        RatingBar(Math.round(stat.avgRating).toInt())
                                        Text("  ${stat.avgRating} (${stat.reviewCount})", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { showReport = true }) { Text("Report") }
                                }
                            }
                        }
                    }

                    if (!canOrder) {
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                            Text(
                                "This restaurant is currently closed for new orders.",
                                modifier = Modifier.padding(12.dp).fillMaxWidth()
                            )
                        }
                    }
                }
            }

            if (cats.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCat == null,
                                onClick = { selectedCat = null },
                                label = { Text("All") }
                            )
                        }
                        items(cats, key = { it.id }) { c ->
                            FilterChip(
                                selected = selectedCat == c.id,
                                onClick = { selectedCat = c.id },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
            }

            items(shown, key = { it.id }) { item ->
                MenuItemRow(item, canOrder) {
                    vm.add(item)
                }
            }
        }

        if (showReport) {
            AlertDialog(
                onDismissRequest = { showReport = false },
                title = { Text("Report Restaurant") },
                text = {
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        placeholder = { Text("Reason...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        vm.report(id, reportText)
                        showReport = false
                        reportText = ""
                    }) { Text("Submit") }
                },
                dismissButton = {
                    TextButton(onClick = { showReport = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem, canOrder: Boolean, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!item.imageUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                if (!item.description.isNullOrBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(money(item.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onAdd, enabled = canOrder && item.isAvailable) {
                Text(if (item.isAvailable) "Add" else "Sold out")
            }
        }
    }
}

// =====================================================================
//  Cart Screen
// =====================================================================
@Composable
private fun CartScreen(vm: CustomerViewModel, nav: NavHostController) {
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    var orderType by remember { mutableStateOf("pickup") }
    var note by remember { mutableStateOf("") }
    var confirmOrder by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (vm.cart.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { confirmOrder = true },
                        modifier = Modifier.fillMaxWidth().padding(12.dp)
                    ) {
                        Text("Place Pre-Order  •  ${money(vm.cartTotal)}")
                    }
                }
            }
        }
    ) { pad ->
        if (vm.cart.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("Your cart is empty")
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vm.cart) { ci ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ci.item.name, fontWeight = FontWeight.Bold)
                                Text(money(ci.item.price), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { vm.remove(ci.item) }) {
                                    Text("-", fontWeight = FontWeight.Bold)
                                }
                                Text("${ci.qty}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { vm.add(ci.item) }) {
                                    Text("+", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Order Type", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = orderType == "pickup", onClick = { orderType = "pickup" }, label = { Text("Pickup") })
                            FilterChip(selected = orderType == "dine_in", onClick = { orderType = "dine_in" }, label = { Text("Dine-in") })
                        }

                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("Add a note (e.g. allergies, extra spicy)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (confirmOrder) {
            ConfirmDialog(
                title = "Confirm Order",
                text = "Are you sure you want to place this order for ${money(vm.cartTotal)}?",
                onConfirm = {
                    confirmOrder = false
                    vm.orderType = orderType
                    vm.note = note
                    vm.placeOrder { orderId ->
                        nav.popBackStack()
                        nav.navigate("order/$orderId")
                    }
                },
                onDismiss = { confirmOrder = false }
            )
        }
    }
}

// =====================================================================
//  Orders Tab
// =====================================================================
@Composable
private fun OrdersTab(vm: CustomerViewModel, nav: NavHostController) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("My Orders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }

        if (vm.orders.isEmpty()) {
            item { EmptyState("No orders yet") }
        }

        items(vm.orders, key = { it.id }) { o ->
            Card(
                onClick = { nav.navigate("order/${o.id}") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Order #${shortId(o.id)}", fontWeight = FontWeight.Bold)
                        StatusChip(o.status)
                    }
                    Text(o.restaurants?.name ?: "Restaurant", style = MaterialTheme.typography.bodyMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(money(o.total), fontWeight = FontWeight.Bold)
                        Text(formatTime(o.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

// =====================================================================
//  Order Detail Screen
// =====================================================================
@Composable
private fun OrderDetailScreen(vm: CustomerViewModel, auth: AuthViewModel, id: String, nav: NavHostController) {
    val o = vm.orders.firstOrNull { it.id == id }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    var showReview by remember { mutableStateOf(false) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (o == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Order not found")
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order #${shortId(o.id)}", fontWeight = FontWeight.Bold)
                                StatusChip(o.status)
                            }
                            Text(o.restaurants?.name ?: "", style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            StatusStepper(o.status)
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Items", fontWeight = FontWeight.Bold)
                            o.items.forEach { item ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${item.quantity}x ${item.itemName}")
                                    Text(money(item.unitPrice * item.quantity))
                                }
                            }
                            HorizontalDivider()
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", fontWeight = FontWeight.Bold)
                                Text(money(o.total), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (o.status == "completed" && o.restaurantId != null) {
                    item {
                        Button(onClick = { showReview = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Write Review")
                        }
                    }
                }
            }

            if (showReview) {
                AlertDialog(
                    onDismissRequest = { showReview = false },
                    title = { Text("Rate Restaurant") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            RatingBar(rating) { rating = it }
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                placeholder = { Text("Leave a comment...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            vm.submitReview(
                                order = o,
                                customerName = auth.profile?.fullName.orEmpty().ifEmpty { "Customer" },
                                rating = rating,
                                comment = comment
                            ) {
                                showReview = false
                            }
                        }) { Text("Submit") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReview = false }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

// =====================================================================
//  Customer Profile Tab
// =====================================================================
@Composable
private fun CustomerProfileTab(vm: CustomerViewModel, auth: AuthViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Name: ${auth.profile?.fullName.orEmpty()}", fontWeight = FontWeight.SemiBold)
                Text("Phone: ${auth.profile?.phone.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                Text("Role: ${auth.profile?.role.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = { auth.logout() }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign Out")
        }
    }
}