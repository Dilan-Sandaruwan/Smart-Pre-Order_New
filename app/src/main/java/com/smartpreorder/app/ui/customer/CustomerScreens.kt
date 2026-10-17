@file:OptIn(ExperimentalMaterial3Api::class)

package com.smartpreorder.app.ui.customer

import com.smartpreorder.app.R

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
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import com.smartpreorder.app.data.MenuItem
import com.smartpreorder.app.data.PortionSize
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.alpha
import com.smartpreorder.app.data.Promotion
import com.smartpreorder.app.ui.parseColorHex
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    Notices(auth, snack)

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            CustomerBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { newTab ->
                    selectedTab = newTab
                    if (nav.currentDestination?.route != "tabs") {
                        nav.popBackStack("tabs", inclusive = false)
                    }
                },
                activeOrderCount = vm.activeOrderCount
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            NavHost(navController = nav, startDestination = "tabs") {
                composable("tabs") {
                    when (selectedTab) {
                        0 -> RestaurantsTab(vm, nav)
                        1 -> OrdersTab(vm, nav)
                        else -> CustomerProfileTab(vm, auth)
                    }
                }
                composable("restaurant/{id}") { entry ->
                    RestaurantMenuScreen(vm, entry.arguments?.getString("id").orEmpty(), nav)
                }
                composable("cart") { CartScreen(vm, nav) }
                composable("order/{id}") { entry ->
                    OrderDetailScreen(vm, auth, entry.arguments?.getString("id").orEmpty(), nav)
                }
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
    val bgHex: Long = 0xFFFFF3E0,
    val iconRes: Int? = null,
    val imageUrl: String? = null
)

val POPULAR_CATEGORIES = listOf(
    PopularCategoryItem("Rice & Curry", "🍛", 0xFFFFF3E0, R.drawable.rice_and_curry),
    PopularCategoryItem("Burgers", "🍔", 0xFFFFEBEE, R.drawable.burgers),
    PopularCategoryItem("Shawarma", "🌯", 0xFFF3E5F5, R.drawable.shawarma),
    PopularCategoryItem("Desserts", "🧁", 0xFFFCE4EC, R.drawable.desserts),
    PopularCategoryItem("Beverages", "🥤", 0xFFE0F7FA, R.drawable.beverages),
    PopularCategoryItem("Cafe", "☕", 0xFFEFEBE9, R.drawable.cafe),
    PopularCategoryItem("Pasta", "🍝", 0xFFFFF8E1, R.drawable.pasta),
    PopularCategoryItem("Juices", "🧃", 0xFFF1F8E9, R.drawable.juices),
    PopularCategoryItem("Bakery", "🥐", 0xFFFFF3E0, R.drawable.bakery),
    PopularCategoryItem("Chinese", "🥢", 0xFFFFEBEE, R.drawable.chinese),
    PopularCategoryItem("Indian", "🥘", 0xFFF3E5F5, R.drawable.indian),
    PopularCategoryItem("Kottu", "🍲", 0xFFFFE0B2, R.drawable.kottu),
    PopularCategoryItem("Pizza", "🍕", 0xFFFFEBEE, R.drawable.pizza),
    PopularCategoryItem("Short Eats", "🥟", 0xFFFFF8E1, R.drawable.short_eats),
    PopularCategoryItem("Fast Food", "🍟", 0xFFFFEBEE, R.drawable.fast_food),
    PopularCategoryItem("Vegetarian", "🥗", 0xFFE8F5E9, R.drawable.vegetarian),
    PopularCategoryItem("Wraps", "🌯", 0xFFF3E5F5, R.drawable.wraps),
    PopularCategoryItem("Noodles", "🍜", 0xFFFFF8E1, R.drawable.noodles),
    PopularCategoryItem("Sea Food", "🦐", 0xFFE0F7FA, R.drawable.seafood)
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
private fun PromotionBanner(promotions: List<Promotion>) {
    val displayList = if (promotions.isNotEmpty()) {
        promotions
    } else {
        listOf(
            Promotion("1", "Big Cravings, Bigger Savings", "Rs. 1000 OFF", "On orders of Rs. 3,000+", null, "#900C3F", "#C70039"),
            Promotion("2", "Special Discount", "20% OFF", "On all Kottu & Burgers today", null, "#1E5128", "#4E9F3D"),
            Promotion("3", "Super Deal", "Free Delivery", "For your first 3 pre-orders", null, "#1F4287", "#278EA5")
        )
    }

    val pagerState = rememberPagerState(pageCount = { displayList.size })

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val promo = displayList[page]
            val bgImage = sanitizeImageUrl(promo.imageUrl)

            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!bgImage.isNullOrBlank()) {
                        AsyncImage(
                            model = bgImage,
                            contentDescription = promo.title ?: "Promotion Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val startColor = parseColorHex(promo.startColor, Color(0xFF900C3F))
                        val endColor = parseColorHex(promo.endColor, Color(0xFFC70039))
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(startColor, endColor)))
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    promo.title?.let {
                                        Text(
                                            it.uppercase(),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    promo.discountText?.let { disc ->
                                        Surface(
                                            color = Color(0xFFFFD700),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                disc,
                                                fontWeight = FontWeight.Black,
                                                style = MaterialTheme.typography.headlineSmall,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                promo.subtext?.let { sub ->
                                    if (sub.isNotBlank()) {
                                        Text(
                                            sub,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (displayList.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(displayList.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(if (pagerState.currentPage == iteration) 8.dp else 5.dp)
                    )
                }
            }
        }
    }
}

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
            PromotionBanner(vm.promotions)
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

                val dbCatNames = vm.categories.map { it.name.lowercase() }.toSet()
                val mergedCategories = vm.categories.map { c ->
                    val defaultItem = POPULAR_CATEGORIES.firstOrNull { it.name.equals(c.name, ignoreCase = true) }
                    PopularCategoryItem(
                        name = c.name,
                        emoji = c.emoji ?: defaultItem?.emoji ?: "🍽️",
                        bgHex = defaultItem?.bgHex ?: 0xFFFFF3E0,
                        iconRes = defaultItem?.iconRes,
                        imageUrl = c.imageUrl ?: defaultItem?.imageUrl
                    )
                } + POPULAR_CATEGORIES.filter { it.name.lowercase() !in dbCatNames }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(mergedCategories, key = { it.name }) { catItem ->
                        val isSelected = selectedCategory == catItem.name
                        val catImage = sanitizeImageUrl(catItem.imageUrl)
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
                                    if (!catImage.isNullOrBlank()) {
                                        AsyncImage(
                                            model = catImage,
                                            contentDescription = catItem.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else if (catItem.iconRes != null) {
                                        AsyncImage(
                                            model = catItem.iconRes,
                                            contentDescription = catItem.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().padding(10.dp).clip(CircleShape)
                                        )
                                    } else {
                                        Text(catItem.emoji, style = MaterialTheme.typography.headlineMedium)
                                    }
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
    var selectingItem by remember { mutableStateOf<MenuItem?>(null) }
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
                    selectingItem = item
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

        selectingItem?.let { item ->
            AddToCartBottomSheet(
                item = item,
                onDismiss = { selectingItem = null },
                onAddToCart = { selections ->
                    selections.forEach { (portion, qty) ->
                        vm.addToCart(item, portion, qty)
                    }
                    selectingItem = null
                }
            )
        }
    }
}

@Composable
private fun MenuItemRow(item: MenuItem, canOrder: Boolean, onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val dishImage = sanitizeImageUrl(item.imageUrl)
            if (!dishImage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = dishImage,
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
                if (item.portionSizes.isNotEmpty()) {
                    Text(
                        item.portionSizes.joinToString(" | ") { "${it.name} - ${money(it.price)}" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(money(item.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
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
                title = { Text("My Cart", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (vm.cart.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = { confirmOrder = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Place Pre-Order  •  ${money(vm.cartTotal)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cart Item Cards
                items(vm.cart) { ci ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dish Image Thumbnail
                            val dishImage = sanitizeImageUrl(ci.item.imageUrl)
                            if (!dishImage.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = dishImage,
                                        contentDescription = ci.item.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                            }

                            // Item details
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    ci.item.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                ci.selectedPortion?.let { p ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            p.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    "${money(ci.unitPrice)}  •  Total: ${money(ci.lineTotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Quantity Selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = { vm.updateCartQty(ci, ci.qty - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (ci.qty == 1) {
                                                Icon(Ic.Delete, "Remove", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                            } else {
                                                Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            }
                                        }
                                    }
                                }

                                Text(
                                    "${ci.qty}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                IconButton(
                                    onClick = { vm.updateCartQty(ci, ci.qty + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Order Type Selector Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Order Type", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (orderType == "pickup") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { orderType = "pickup" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🛍️ Pickup", fontWeight = if (orderType == "pickup") FontWeight.Bold else FontWeight.Normal)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (orderType == "dine_in") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { orderType = "dine_in" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🍽️ Dine-in", fontWeight = if (orderType == "dine_in") FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }

                // Instructions / Note Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Special Instructions", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = note,
                                onValueChange = { note = it },
                                placeholder = { Text("Add notes (e.g. extra spicy, no cutlery)") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bill Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Bill Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Items Total", color = MaterialTheme.colorScheme.outline)
                                Text(money(vm.cartTotal), fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Amount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    money(vm.cartTotal),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
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
    val activeList = vm.orders.filter { it.status in ACTIVE_STATUSES }
    val completedList = vm.orders.filter { it.status !in ACTIVE_STATUSES }

    var selectedFilter by remember { mutableStateOf(if (activeList.isNotEmpty()) "active" else "all") }

    val filteredOrders = when (selectedFilter) {
        "active" -> activeList
        "completed" -> completedList
        else -> vm.orders
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("My Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedFilter == "active",
                        onClick = { selectedFilter = "active" },
                        label = { Text("Active (${activeList.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "completed",
                        onClick = { selectedFilter = "completed" },
                        label = { Text("Completed (${completedList.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text("All (${vm.orders.size})") }
                    )
                }
            }
        }

        if (filteredOrders.isEmpty()) {
            item {
                val emptyText = when (selectedFilter) {
                    "active" -> "No live active orders right now."
                    "completed" -> "No completed past orders yet."
                    else -> "No orders yet."
                }
                EmptyState(emptyText)
            }
        }

        items(filteredOrders, key = { it.id }) { o ->
            val isActiveOrder = o.status in ACTIVE_STATUSES
            Card(
                onClick = { nav.navigate("order/${o.id}") },
                shape = RoundedCornerShape(16.dp),
                colors = if (isActiveOrder) {
                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                } else {
                    CardDefaults.cardColors()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Order ${shortId(o.id)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        StatusChip(o.status)
                    }

                    Text(
                        o.restaurants?.name ?: "Restaurant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallTag(orderTypeLabel(o.orderType))
                        if (!o.tableNo.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    "Table No: ${o.tableNo}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (o.items.isNotEmpty()) {
                        Text(
                            o.items.joinToString(", ") { "${it.quantity}× ${it.itemName}" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatTime(o.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(money(o.total), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
    var existingReview by remember { mutableStateOf<Review?>(null) }
    LaunchedEffect(id) {
        vm.refreshOrders()
        existingReview = vm.reviewFor(id)
    }
    val o = vm.orders.firstOrNull { it.id == id }
    val snack = remember { SnackbarHostState() }
    Notices(vm, snack)
    var showReview by remember { mutableStateOf(false) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Ic.Back, "Back") } }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        if (o == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState("Order not found")
            }
        } else {
            LazyColumn(
                Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Table Assigned Banner for Dine-in
                if (!o.tableNo.isNullOrBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🍽️", style = MaterialTheme.typography.headlineMedium)
                                Column {
                                    Text(
                                        if (o.status == "ready" || o.status == "completed")
                                            "Your order is ready!"
                                        else
                                            "Dine-in Table Assigned",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Please be seated at Table No: ${o.tableNo}",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Status / Order Info Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Order ${shortId(o.id)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                StatusChip(o.status)
                            }

                            Text(
                                o.restaurants?.name ?: "Restaurant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SmallTag(orderTypeLabel(o.orderType))
                                if (!o.tableNo.isNullOrBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            "Table No: ${o.tableNo}",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(
                                    formatTime(o.createdAt),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            HorizontalDivider()

                            // Modern Timeline Stepper
                            StatusStepper(o.status)
                        }
                    }
                }

                // Ordered Items Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Ordered Items", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()

                            o.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "${item.quantity}x",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Text(
                                            item.itemName,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(
                                        money(item.unitPrice * item.quantity),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }

                            if (!o.note.isNullOrBlank()) {
                                HorizontalDivider()
                                Text("📝 Note: ${o.note}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Payment Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Payment Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Subtotal", color = MaterialTheme.colorScheme.outline)
                                Text(money(o.total), fontWeight = FontWeight.SemiBold)
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Amount Paid", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    money(o.total),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Rating & Review for Completed Orders
                if (o.status == "completed") {
                    item {
                        if (existingReview != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("Your Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RatingBar(existingReview!!.rating)
                                        Text(" (${existingReview!!.rating}/5)", fontWeight = FontWeight.Bold)
                                    }
                                    if (!existingReview!!.comment.isNullOrBlank()) {
                                        Text("\"${existingReview!!.comment}\"", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = { showReview = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("⭐ Rate & Review Restaurant", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showReview) {
                AlertDialog(
                    onDismissRequest = { showReview = false },
                    title = { Text("Rate Restaurant", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            RatingBar(rating) { rating = it }
                            OutlinedTextField(
                                value = comment,
                                onValueChange = { comment = it },
                                placeholder = { Text("Leave a comment (optional)...") },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                vm.submitReview(
                                    order = o,
                                    customerName = auth.profile?.fullName.orEmpty().ifEmpty { "Customer" },
                                    rating = rating,
                                    comment = comment
                                ) { rev ->
                                    existingReview = rev
                                    showReview = false
                                }
                            },
                            enabled = !vm.busy
                        ) {
                            Text(if (vm.busy) "Submitting..." else "Submit Review")
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToCartBottomSheet(
    item: MenuItem,
    onDismiss: () -> Unit,
    onAddToCart: (List<Pair<PortionSize?, Int>>) -> Unit
) {
    val portionQuantities = remember {
        mutableStateMapOf<String, Int>().apply {
            item.portionSizes.forEachIndexed { idx, p ->
                put(p.name, if (idx == 0) 1 else 0)
            }
        }
    }
    var baseQuantity by remember { mutableIntStateOf(1) }

    val isPortionMode = item.portionSizes.isNotEmpty()

    val totalPrice = if (isPortionMode) {
        item.portionSizes.sumOf { p -> (portionQuantities[p.name] ?: 0) * p.price }
    } else {
        item.price * baseQuantity
    }

    val totalCount = if (isPortionMode) {
        item.portionSizes.sumOf { p -> portionQuantities[p.name] ?: 0 }
    } else {
        baseQuantity
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val dishImage = sanitizeImageUrl(item.imageUrl)
                if (!dishImage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = dishImage,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!item.description.isNullOrBlank()) {
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            HorizontalDivider()

            if (isPortionMode) {
                Text("Select Portion Sizes & Quantity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item.portionSizes.forEach { p ->
                        val qty = portionQuantities[p.name] ?: 0
                        val isSelected = qty > 0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        p.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        money(p.price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (qty > 0) portionQuantities[p.name] = qty - 1
                                        },
                                        enabled = qty > 0,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (qty > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            }
                                        }
                                    }

                                    Text(
                                        "$qty",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            portionQuantities[p.name] = qty + 1
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Quantity Selection for single price item
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Quantity", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(money(item.price), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = { if (baseQuantity > 1) baseQuantity-- },
                            enabled = baseQuantity > 1
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        Text("$baseQuantity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                        IconButton(onClick = { baseQuantity++ }) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Add to Cart Button
            Button(
                onClick = {
                    if (isPortionMode) {
                        val selections = item.portionSizes.mapNotNull { p ->
                            val q = portionQuantities[p.name] ?: 0
                            if (q > 0) p to q else null
                        }
                        onAddToCart(selections)
                    } else {
                        onAddToCart(listOf(null to baseQuantity))
                    }
                },
                enabled = totalCount > 0,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (totalCount > 0) "Add to Cart  •  ${money(totalPrice)}" else "Select quantity",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}