package com.smartpreorder.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartpreorder.app.data.*
import com.smartpreorder.app.notifications.Notifier
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

abstract class BaseVm : ViewModel() {
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)

    protected fun launchSafe(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.friendly()
        }
    }
}

// =====================================================================
//  AUTH
// =====================================================================
class AuthViewModel : BaseVm() {
    var loading by mutableStateOf(true)
    var profile by mutableStateOf<Profile?>(null)
    var info by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { st ->
                when (st) {
                    is SessionStatus.Authenticated -> loadProfile()
                    is SessionStatus.NotAuthenticated -> {
                        profile = null
                        loading = false
                    }
                    else -> if (st !is SessionStatus.Initializing) loading = false
                }
            }
        }
    }

    private suspend fun loadProfile() {
        try {
            val p = Repo.myProfile()
            if (p.isBlocked) {
                error = "Your account has been blocked by the administrator."
                Repo.signOut()
                profile = null
            } else {
                profile = p
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.friendly()
            profile = null
        }
        loading = false
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            error = "Enter your e-mail and password"; return
        }
        launchSafe {
            busy = true; info = null
            try {
                Repo.signIn(email.trim(), password)
            } finally {
                busy = false
            }
        }
    }

    fun register(email: String, password: String, name: String, phone: String, role: String) {
        when {
            name.isBlank() -> { error = "Enter your name"; return }
            email.isBlank() -> { error = "Enter your e-mail"; return }
            password.length < 6 -> { error = "Password must be at least 6 characters"; return }
        }
        launchSafe {
            busy = true; info = null
            try {
                val signedIn = Repo.signUp(email.trim(), password, name.trim(), phone.trim(), role)
                if (!signedIn) info = "Account created. Check your e-mail to confirm it, then log in."
            } finally {
                busy = false
            }
        }
    }

    fun updateProfile(name: String, phone: String) = launchSafe {
        Repo.updateProfile(name.trim(), phone.trim())
        profile = Repo.myProfile()
        message = "Profile saved"
    }

    fun logout() {
        launchSafe { Repo.signOut() }
    }
}

// =====================================================================
//  CUSTOMER
// =====================================================================
data class CartLine(
    val item: MenuItem,
    val selectedPortion: PortionSize? = null,
    val qty: Int = 1
) {
    val unitPrice: Double get() = selectedPortion?.price ?: item.price
    val displayName: String get() = if (selectedPortion != null) "${item.name} (${selectedPortion.name})" else item.name
    val lineTotal: Double get() = unitPrice * qty
}

class CustomerViewModel : BaseVm() {
    var restaurants by mutableStateOf<List<Restaurant>>(emptyList())
    var stats by mutableStateOf<Map<String, RestaurantStat>>(emptyMap())
    var categories by mutableStateOf<List<Category>>(emptyList())
    var loadingRestaurants by mutableStateOf(true)

    var query by mutableStateOf("")
    var dishResults by mutableStateOf<List<MenuItem>>(emptyList())
    private var searchJob: Job? = null

    var menu by mutableStateOf<List<MenuItem>>(emptyList())
    var reviews by mutableStateOf<List<Review>>(emptyList())
    var menuLoading by mutableStateOf(false)

    val cart = mutableStateListOf<CartLine>()
    var cartRestaurantId by mutableStateOf<String?>(null)
    var orderType by mutableStateOf("pickup")
    var note by mutableStateOf("")

    var orders by mutableStateOf<List<Order>>(emptyList())
    private var lastStatus: Map<String, String>? = null
    private var lastTableNo: Map<String, String?>? = null

    val cartCount get() = cart.sumOf { it.qty }
    val cartTotal get() = cart.sumOf { it.lineTotal }
    val activeOrderCount get() = orders.count { it.status in ACTIVE_STATUSES }

    /** Called from a LaunchedEffect: loads data, then listens for realtime order updates. */
    suspend fun start() {
        refreshRestaurants()
        try {
            loadOrders(false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.friendly()
        }
        while (true) {
            try {
                Repo.orderChanges().collect { loadOrders(true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // connection dropped - retry below
            }
            delay(4000)
        }
    }

    private suspend fun loadOrders(notify: Boolean) {
        val list = Repo.myOrders()
        val prev = lastStatus
        val prevTable = lastTableNo
        if (notify && prev != null) {
            list.forEach { o ->
                val oldSt = prev[o.id]
                val oldT = prevTable?.get(o.id)
                if (oldSt != null && (oldSt != o.status || oldT != o.tableNo)) {
                    Notifier.statusChanged(o)
                }
            }
        }
        lastStatus = list.associate { it.id to it.status }
        lastTableNo = list.associate { it.id to it.tableNo }
        orders = list
    }

    var promotions by mutableStateOf<List<Promotion>>(emptyList())

    fun refreshOrders() = launchSafe { loadOrders(false) }

    fun refreshRestaurants() = launchSafe {
        try {
            restaurants = Repo.restaurants()
            stats = orNull { Repo.restaurantStats() }.orEmpty().associateBy { it.restaurantId }
            categories = orNull { Repo.categories() }.orEmpty()
            promotions = Repo.promotions()
        } finally {
            loadingRestaurants = false
        }
    }

    fun onQuery(q: String) {
        query = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            dishResults = if (q.trim().length >= 2) orNull { Repo.searchDishes(q.trim()) }.orEmpty() else emptyList()
        }
    }

    fun loadMenu(restaurantId: String) = launchSafe {
        menuLoading = true
        try {
            menu = emptyList()
            reviews = emptyList()
            menu = Repo.menu(restaurantId)
            reviews = orNull { Repo.reviewsFor(restaurantId) }.orEmpty()
        } finally {
            menuLoading = false
        }
    }

    // ---- cart
    fun qty(itemId: String) = cart.filter { it.item.id == itemId }.sumOf { it.qty }

    fun addToCart(item: MenuItem, portion: PortionSize? = null, qty: Int = 1) {
        if (cartRestaurantId != null && cartRestaurantId != item.restaurantId) {
            cart.clear()
            message = "Cart cleared - you can order from one restaurant at a time."
        }
        cartRestaurantId = item.restaurantId
        val i = cart.indexOfFirst {
            it.item.id == item.id && it.selectedPortion?.name == portion?.name
        }
        if (i >= 0) {
            val existing = cart[i]
            cart[i] = existing.copy(qty = existing.qty + qty)
        } else {
            cart.add(CartLine(item, portion, qty))
        }
    }

    fun updateCartQty(line: CartLine, newQty: Int) {
        val i = cart.indexOf(line)
        if (i < 0) return
        if (newQty <= 0) {
            cart.removeAt(i)
        } else {
            cart[i] = cart[i].copy(qty = newQty)
        }
        if (cart.isEmpty()) cartRestaurantId = null
    }

    fun placeOrder(onDone: (String) -> Unit) {
        val rid = cartRestaurantId ?: return
        launchSafe {
            busy = true
            try {
                val dtoList = cart.map {
                    CartLineDto(
                        menuItemId = it.item.id,
                        quantity = it.qty,
                        portionName = it.selectedPortion?.name,
                        unitPrice = it.unitPrice
                    )
                }
                val id = Repo.placeOrder(rid, orderType, note, dtoList)
                cart.clear()
                cartRestaurantId = null
                note = ""
                loadOrders(false)
                onDone(id)
            } finally {
                busy = false
            }
        }
    }

    fun cancel(orderId: String) = launchSafe {
        Repo.cancelOrder(orderId)
        loadOrders(false)
        message = "Order cancelled"
    }

    suspend fun reviewFor(orderId: String): Review? = orNull { Repo.reviewForOrder(orderId) }

    fun submitReview(order: Order, customerName: String, rating: Int, comment: String, onDone: (Review?) -> Unit) =
        launchSafe {
            Repo.submitReview(order, customerName, rating, comment)
            message = "Thanks for your review!"
            stats = orNull { Repo.restaurantStats() }.orEmpty().associateBy { it.restaurantId }
            onDone(Repo.reviewForOrder(order.id))
        }

    fun report(restaurantId: String, reason: String) = launchSafe {
        Repo.report(restaurantId, reason.trim())
        message = "Report sent to the admin"
    }
}

// =====================================================================
//  RESTAURANT
// =====================================================================
class RestaurantViewModel : BaseVm() {
    var loaded by mutableStateOf(false)
    var restaurant by mutableStateOf<Restaurant?>(null)
    var orders by mutableStateOf<List<Order>>(emptyList())
    var menu by mutableStateOf<List<MenuItem>>(emptyList())
    var categories by mutableStateOf<List<Category>>(emptyList())
    private var seen: Set<String>? = null

    val activeOrders get() = orders.filter { it.status in ACTIVE_STATUSES }.sortedBy { it.createdAt }
    val historyOrders get() = orders.filter { it.status !in ACTIVE_STATUSES }
    val completed get() = orders.filter { it.status == "completed" }
    val totalSales get() = completed.sumOf { it.total }
    val todayCompleted get() = completed.filter { isToday(it.createdAt) }
    val pendingCount get() = orders.count { it.status == "pending" }

    suspend fun start() {
        try {
            restaurant = Repo.myRestaurant()
            categories = orNull { Repo.categories() }.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.friendly()
        }
        loaded = true
        // (re)start live updates whenever the restaurant record appears
        snapshotFlow { restaurant?.id }.filterNotNull().distinctUntilChanged().collectLatest { rid ->
            try {
                loadOrders(rid, false)
                menu = Repo.menu(rid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.friendly()
            }
            while (true) {
                try {
                    Repo.orderChanges().collect { loadOrders(rid, true) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // reconnect below
                }
                delay(4000)
            }
        }
    }

    var salesReports by mutableStateOf<List<SalesReport>>(emptyList())

    private suspend fun loadOrders(rid: String, notify: Boolean) {
        val list = Repo.restaurantOrders(rid)
        val prev = seen
        if (notify && prev != null) {
            list.filter { it.status == "pending" && it.id !in prev }.forEach { Notifier.newOrder(it) }
        }
        seen = list.map { it.id }.toSet()
        orders = list
        salesReports = Repo.salesReports(rid)
    }

    fun saveSalesReport(periodType: String, periodLabel: String, totalSales: Double, totalOrders: Int, fileUrl: String?) {
        val r = restaurant ?: return
        launchSafe {
            Repo.saveSalesReport(
                NewSalesReport(
                    restaurantId = r.id,
                    periodType = periodType,
                    periodLabel = periodLabel,
                    totalSales = totalSales,
                    totalOrders = totalOrders,
                    fileUrl = fileUrl
                )
            )
            salesReports = Repo.salesReports(r.id)
            message = "📊 Sales report saved to database!"
        }
    }

    fun refresh() = launchSafe {
        val r = restaurant ?: return@launchSafe
        loadOrders(r.id, false)
        menu = Repo.menu(r.id)
    }

    fun createRestaurant(
        name: String,
        description: String,
        address: String,
        phone: String,
        email: String = "",
        categoryId: String? = null,
        imageUrl: String? = null,
        coverImageUrl: String? = null,
        city: String = "",
        district: String = "",
        postalCode: String = "",
        businessRegistrationNo: String = "",
        minOrderAmount: Double = 0.0,
        estimatedPrepTimeMinutes: Int? = null,
        deliveryRadiusKm: Double? = null,
    ) {
        if (name.isBlank()) { error = "Enter the restaurant name"; return }
        launchSafe {
            busy = true
            try {
                restaurant = Repo.createRestaurant(
                    name = name.trim(),
                    description = description.trim(),
                    address = address.trim(),
                    phone = phone.trim(),
                    email = email.trim(),
                    categoryId = categoryId,
                    imageUrl = imageUrl,
                    coverImageUrl = coverImageUrl,
                    city = city.trim(),
                    district = district.trim(),
                    postalCode = postalCode.trim(),
                    businessRegistrationNo = businessRegistrationNo.trim(),
                    minOrderAmount = minOrderAmount,
                    estimatedPrepTimeMinutes = estimatedPrepTimeMinutes,
                    deliveryRadiusKm = deliveryRadiusKm,
                )
            } finally {
                busy = false
            }
        }
    }

    fun updateRestaurant(
        name: String,
        description: String,
        address: String,
        phone: String,
        email: String = "",
        categoryId: String? = null,
        imageUrl: String? = null,
        coverImageUrl: String? = null,
        city: String = "",
        district: String = "",
        postalCode: String = "",
        businessRegistrationNo: String = "",
        minOrderAmount: Double = 0.0,
        estimatedPrepTimeMinutes: Int? = null,
        deliveryRadiusKm: Double? = null,
        isOpen: Boolean = true,
        reSubmitApproval: Boolean = false,
    ) {
        val r = restaurant ?: return
        if (name.isBlank()) { error = "Enter the restaurant name"; return }
        launchSafe {
            Repo.updateRestaurant(
                id = r.id,
                name = name.trim(),
                description = description.trim(),
                address = address.trim(),
                phone = phone.trim(),
                email = email.trim(),
                categoryId = categoryId,
                imageUrl = imageUrl,
                coverImageUrl = coverImageUrl,
                city = city.trim(),
                district = district.trim(),
                postalCode = postalCode.trim(),
                businessRegistrationNo = businessRegistrationNo.trim(),
                minOrderAmount = minOrderAmount,
                estimatedPrepTimeMinutes = estimatedPrepTimeMinutes,
                deliveryRadiusKm = deliveryRadiusKm,
                isOpen = isOpen,
                reSubmitApproval = reSubmitApproval,
            )
            restaurant = Repo.myRestaurant()
            message = if (reSubmitApproval) "Profile updated and re-submitted for Admin approval!" else "Saved"
        }
    }

    fun setStatus(order: Order, status: String, minutes: Int? = null, reason: String? = null, tableNo: String? = null) = launchSafe {
        Repo.setOrderStatus(order.id, status, minutes, reason, tableNo)
        restaurant?.let { loadOrders(it.id, false) }
    }

    fun setEta(order: Order, minutes: Int) = launchSafe {
        Repo.setOrderEta(order.id, minutes)
        restaurant?.let { loadOrders(it.id, false) }
    }

    fun saveItem(
        id: String?,
        categoryId: String?,
        categoryName: String?,
        name: String,
        description: String,
        price: Double,
        portionSizes: List<PortionSize>,
        available: Boolean,
        imageUrl: String? = null
    ) {
        val r = restaurant ?: return
        launchSafe {
            Repo.saveMenuItem(id, r.id, categoryId, categoryName, name.trim(), description.trim(), price, portionSizes, available, imageUrl)
            menu = Repo.menu(r.id)
        }
    }

    fun toggleAvailable(item: MenuItem, available: Boolean) = launchSafe {
        Repo.setItemAvailable(item.id, available)
        restaurant?.let { menu = Repo.menu(it.id) }
    }

    fun deleteItem(item: MenuItem) = launchSafe {
        Repo.deleteMenuItem(item.id)
        restaurant?.let { menu = Repo.menu(it.id) }
    }
}

// =====================================================================
//  ADMIN
// =====================================================================
class AdminViewModel : BaseVm() {
    var stats by mutableStateOf(AdminStats())
    var restaurants by mutableStateOf<List<Restaurant>>(emptyList())
    var users by mutableStateOf<List<Profile>>(emptyList())
    var orders by mutableStateOf<List<Order>>(emptyList())
    var categories by mutableStateOf<List<Category>>(emptyList())
    var reports by mutableStateOf<List<Report>>(emptyList())

    var promotions by mutableStateOf<List<Promotion>>(emptyList())

    fun refresh() = launchSafe {
        val p = Repo.myProfile()
        if (p.role != "admin") {
            error = "Admin permission required. Please update role = 'admin' in Supabase public.profiles table."
            return@launchSafe
        }
        stats = orNull { Repo.adminStats() } ?: AdminStats()
        restaurants = Repo.allRestaurants()
        users = Repo.allProfiles()
        orders = Repo.allOrders()
        categories = Repo.categories()
        reports = Repo.openReports()
        promotions = Repo.allPromotions()
    }

    fun setRestaurantActive(r: Restaurant, active: Boolean) = launchSafe {
        Repo.setRestaurantActive(r.id, active)
        restaurants = Repo.allRestaurants()
    }

    fun approveRestaurant(r: Restaurant, approved: Boolean) = launchSafe {
        Repo.approveRestaurant(r.id, approved)
        restaurants = Repo.allRestaurants()
        message = if (approved) "${r.name} approved!" else "${r.name} rejected"
    }

    fun setBlocked(p: Profile, blocked: Boolean) = launchSafe {
        Repo.setBlocked(p.id, blocked)
        users = Repo.allProfiles()
    }

    fun addCategory(name: String, imageUrl: String? = null, emoji: String? = null) {
        if (name.isBlank()) return
        launchSafe {
            Repo.addCategory(name.trim(), imageUrl?.ifBlank { null }, emoji?.ifBlank { null })
            categories = Repo.categories()
            message = "Category added!"
        }
    }

    fun deleteCategory(c: Category) = launchSafe {
        Repo.deleteCategory(c.id)
        categories = Repo.categories()
    }

    fun resolveReport(r: Report) = launchSafe {
        Repo.resolveReport(r.id)
        reports = Repo.openReports()
    }

    fun addPromotion(
        imageUrl: String,
        title: String? = null,
        discountText: String? = null,
        subtext: String? = null,
        startColor: String = "#900C3F",
        endColor: String = "#C70039"
    ) {
        if (imageUrl.isBlank()) {
            error = "Please select and upload a banner image"
            return
        }
        launchSafe {
            Repo.addPromotion(
                NewPromotion(
                    title = title?.trim()?.ifBlank { null } ?: "Promotion Banner",
                    discountText = discountText?.trim()?.ifBlank { null },
                    subtext = subtext?.trim()?.ifBlank { null },
                    imageUrl = imageUrl.trim(),
                    startColor = startColor,
                    endColor = endColor
                )
            )
            promotions = Repo.allPromotions()
            message = "Banner added successfully!"
        }
    }

    fun deletePromotion(promo: Promotion) {
        launchSafe {
            Repo.deletePromotion(promo.id)
            promotions = Repo.allPromotions()
            message = "Promotion deleted!"
        }
    }
}
