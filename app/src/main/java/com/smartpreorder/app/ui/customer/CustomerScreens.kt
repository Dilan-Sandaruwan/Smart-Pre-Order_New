@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.customer

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
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
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartpreorder.app.data.MenuItem
import com.smartpreorder.app.data.Order
import com.smartpreorder.app.data.Profile
import com.smartpreorder.app.data.Repo
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
        // 1. Top Header & Search Bar
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

        // 2. Offers Banner Carousel
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

        // 3. Popular Categories Section
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

        // 4. Dish Search Results (if searching)
        if (vm.dishResults.isNotEmpty()) {
            item { Section("Dishes") }
            items(vm.dishResults, key = { "dish-" + it.id }) { d ->
                Card(onClick = { nav.navigate("restaurant/${d.restaurantId}") }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.SemiBold)
                            Text(d.restaurants?.name ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(money(d.price), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5. Restaurants List
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
                // Cover photo
                if (!r.coverImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = r.coverImageUrl,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!r.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = r.imageUrl,
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
                        Text("🍽️ ${r.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Open/Closed Badge
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

                // Profile Picture / Logo Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp)
                        .offset(y = 20.dp)
                        .size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (!r.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = r.imageUrl,
                                contentDescription = "Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            Text(
                                r.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
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
                                if (!r.coverImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = r.coverImageUrl,
                                        contentDescription = "Cover",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (!r.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = r.imageUrl,
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
                                        if (!r.imageUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = r.imageUrl,
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
                            Text("This restaurant is currently closed for new orders.",
                                modifier = Modifier.padding(12.dp).fillMaxWidth())
                        }
                    }
                }
            }
            if (cats.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(selected = selectedCat == null, onClick = { selectedCat = null }, label = { Text("All") }) }
                        items(cats, key = { it.id }) { c ->
                            FilterChip(selected = selectedCat == c.id, onClick = { selectedCat = c.id }, label = { Text(c.name) })
                        }
                    }
                }
            }
            if (vm.menuLoading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            else if (shown.isEmpty()) item { EmptyState("No items on the menu yet") }
            items(shown, key = { it.id }) { item -> MenuItemRow(vm, item, canOrder) }

            if (vm.reviews.isNotEmpty()) {
                item { Section("Reviews") }
                items(vm.reviews, key = { "rev-" + it.id }) { rv -> ReviewRow(rv) }
            }
        }
    }

    if (showReport) {
        AlertDialog(
            onDismissRequest = { showReport = false },
            title = { Text("Report this restaurant") },
            text = {
                OutlinedTextField(reportText, { reportText = it }, label = { Text("What's the problem?") },
                    modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    if (reportText.isNotBlank()) vm.report(id, reportText)
                    reportText = ""; showReport = false
                }) { Text("Send") }
            },
            dismissButton = { TextButton(onClick = { showReport = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MenuItemRow(vm: CustomerViewModel, item: MenuItem, canOrder: Boolean) {
    val qty = vm.qty(item.id)
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                if (!item.description.isNullOrBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Text(money(item.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            when {
                !item.isAvailable -> SmallTag("Sold out")
                !canOrder -> {}
                qty == 0 -> FilledTonalIconButton(onClick = { vm.add(item) }) { Icon(Ic.Add, "Add") }
                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.remove(item) }) { Icon(Ic.Remove, "Less") }
                    Text("$qty", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { vm.add(item) }) { Icon(Ic.Add, "More") }
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(rv: Review) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingBar(rv.rating)
            Text("  " + (rv.customerName ?: "Customer"), style = MaterialTheme.typography.labelMedium)
        }
        if (!rv.comment.isNullOrBlank()) Text(rv.comment, style = MaterialTheme.typography.bodyMedium)
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

// =====================================================================
//  Cart + checkout
// =====================================================================
@Composable
private fun CartScreen(vm: CustomerViewModel, nav: NavHostController) {
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    val restaurantName = vm.restaurants.firstOrNull { it.id == vm.cartRestaurantId }?.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your cart") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (vm.cart.isEmpty()) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("Your cart is empty.\nPick something tasty from a menu!")
            }
        } else {
            Column(
                Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (restaurantName != null) Text("From $restaurantName", style = MaterialTheme.typography.titleMedium)
                vm.cart.toList().forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(line.item.name, fontWeight = FontWeight.SemiBold)
                            Text(money(line.item.price), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { vm.remove(line.item) }) { Icon(Ic.Remove, "Less") }
                        Text("${line.qty}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = { vm.add(line.item) }) { Icon(Ic.Add, "More") }
                        Text(money(line.item.price * line.qty), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                HorizontalDivider()
                Section("How will you eat?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = vm.orderType == "pickup", onClick = { vm.orderType = "pickup" },
                        label = { Text("Pickup") })
                    FilterChip(selected = vm.orderType == "dine_in", onClick = { vm.orderType = "dine_in" },
                        label = { Text("Dine-in") })
                }
                OutlinedTextField(vm.note, { vm.note = it }, label = { Text("Note for the kitchen (optional)") },
                    modifier = Modifier.fillMaxWidth())
                HorizontalDivider()
                Row {
                    Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(money(vm.cartTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        vm.placeOrder { id ->
                            nav.navigate("order/$id") { popUpTo("tabs") }
                        }
                    },
                    enabled = !vm.busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (vm.busy) "Placing order..." else "Place order") }
                Text("Pay at the restaurant. The restaurant will confirm your order and give an estimated time.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// =====================================================================
//  Tab 2: orders + tracking
// =====================================================================
@Composable
private fun OrdersTab(vm: CustomerViewModel, nav: NavHostController) {
    val active = vm.orders.filter { it.status in ACTIVE_STATUSES }
    val past = vm.orders.filter { it.status !in ACTIVE_STATUSES }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("My orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { vm.refreshOrders() }) { Icon(Ic.Refresh, "Refresh") }
            }
        }
        if (vm.orders.isEmpty()) item { EmptyState("No orders yet.") }
        if (active.isNotEmpty()) {
            item { Section("Active") }
            items(active, key = { it.id }) { CustomerOrderCard(it) { nav.navigate("order/${it.id}") } }
        }
        if (past.isNotEmpty()) {
            item { Section("History") }
            items(past, key = { it.id }) { CustomerOrderCard(it) { nav.navigate("order/${it.id}") } }
        }
    }
}

@Composable
private fun CustomerOrderCard(o: Order, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.restaurants?.name ?: "Restaurant", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                StatusChip(o.status)
            }
            Text(o.items.joinToString { "${it.quantity}× ${it.itemName}" }, style = MaterialTheme.typography.bodyMedium)
            Row {
                Text("${shortId(o.id)} • ${orderTypeLabel(o.orderType)} • ${formatTime(o.createdAt)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f))
                Text(money(o.total), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OrderDetailScreen(vm: CustomerViewModel, auth: AuthViewModel, id: String, nav: NavHostController) {
    val order = vm.orders.firstOrNull { it.id == id }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    var confirmCancel by remember { mutableStateOf(false) }
    var myReview by remember { mutableStateOf<Review?>(null) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(order?.status) {
        val o = order
        if (o != null && o.status == "completed") myReview = vm.reviewFor(o.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (order != null) "Order ${shortId(order.id)}" else "Order") },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (order == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(order.restaurants?.name ?: "Restaurant", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusChip(order.status)
                SmallTag(orderTypeLabel(order.orderType))
                Text(formatTime(order.createdAt), style = MaterialTheme.typography.bodySmall)
            }

            if (order.status in ACTIVE_STATUSES || order.status == "completed") {
                StatusStepper(order.status)
            }
            when (order.status) {
                "pending" -> Text("Waiting for the restaurant to accept your order...")
                "accepted", "preparing" -> Text(
                    order.estimatedMinutes?.let { "Estimated preparation time: about $it minutes." }
                        ?: "The restaurant is working on your order."
                )
                "ready" -> Text("Your food is ready! Please come to the restaurant.", fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary)
                "rejected" -> Text("The restaurant could not accept this order." +
                    (order.rejectReason?.let { "\nReason: $it" } ?: ""), color = MaterialTheme.colorScheme.error)
                "cancelled" -> Text("You cancelled this order.")
            }

            HorizontalDivider()
            order.items.forEach { it2 ->
                Row {
                    Text("${it2.quantity}× ${it2.itemName}", modifier = Modifier.weight(1f))
                    Text(money(it2.unitPrice * it2.quantity))
                }
            }
            HorizontalDivider()
            Row {
                Text("Total", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(money(order.total), fontWeight = FontWeight.Bold)
            }
            if (!order.note.isNullOrBlank()) Text("Note: ${order.note}", color = MaterialTheme.colorScheme.outline)

            if (order.status == "pending") {
                OutlinedButton(onClick = { confirmCancel = true }, modifier = Modifier.fillMaxWidth()) { Text("Cancel order") }
            }

            if (order.status == "completed") {
                HorizontalDivider()
                if (myReview != null) {
                    Section("Your review")
                    RatingBar(myReview!!.rating)
                    if (!myReview!!.comment.isNullOrBlank()) Text(myReview!!.comment!!)
                } else {
                    Section("How was your meal?")
                    RatingBar(rating) { rating = it }
                    OutlinedTextField(comment, { comment = it }, label = { Text("Comment (optional)") },
                        modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            vm.submitReview(order, auth.profile?.fullName ?: "Customer", rating, comment) { myReview = it }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Submit review") }
                }
            }
        }
    }

    if (confirmCancel && order != null) {
        ConfirmDialog("Cancel order?", "The restaurant hasn't accepted it yet, so you can cancel it for free.",
            "Cancel order", onConfirm = { vm.cancel(order.id) }, onDismiss = { confirmCancel = false })
    }
}

// =====================================================================
//  Tab 3: profile
// =====================================================================
@Composable
private fun CustomerProfileTab(vm: CustomerViewModel, auth: AuthViewModel) {
    val p = auth.profile
    var name by remember(p?.id) { mutableStateOf(p?.fullName.orEmpty()) }
    var phone by remember(p?.id) { mutableStateOf(p?.phone.orEmpty()) }
    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("My profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(Repo.email(), color = MaterialTheme.colorScheme.outline)
        OutlinedTextField(name, { name = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { auth.updateProfile(name, phone) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        OutlinedButton(onClick = { auth.logout() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Ic.Logout, null)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text("Log out")
        }
    }
}
