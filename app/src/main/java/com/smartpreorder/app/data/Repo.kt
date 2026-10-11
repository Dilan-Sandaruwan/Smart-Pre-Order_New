package com.smartpreorder.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order as SortOrder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Runs [block]; returns null on any failure except coroutine cancellation. */
inline fun <T> orNull(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

/** Every Supabase call in the app goes through here. */
object Repo {
    private val db get() = supabase.postgrest
    private const val ORDER_COLS = "*, order_items(*), restaurants(name)"

    fun uid(): String = supabase.auth.currentUserOrNull()?.id ?: error("Please sign in again")
    fun email(): String = supabase.auth.currentUserOrNull()?.email ?: ""

    // ------------------------------------------------------------ auth
    suspend fun signIn(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /** Returns true if a session was created (false => e-mail confirmation required). */
    suspend fun signUp(email: String, password: String, name: String, phone: String, role: String): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", name)
                put("phone", phone)
                put("role", role)
            }
        }
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun signOut() = supabase.auth.signOut()

    suspend fun myProfile(): Profile =
        db.from("profiles").select { filter { eq("id", uid()) } }.decodeSingle<Profile>()

    suspend fun updateProfile(name: String, phone: String) {
        db.from("profiles").update(buildJsonObject {
            put("full_name", name)
            put("phone", phone)
        }) { filter { eq("id", uid()) } }
    }

    // -------------------------------------------------------- customer
    suspend fun restaurants(): List<Restaurant> =
        db.from("restaurants").select {
            filter {
                eq("is_active", true)
                eq("approval_status", "approved")
            }
            order("name", SortOrder.ASCENDING)
        }.decodeList()

    suspend fun restaurantStats(): List<RestaurantStat> =
        db.from("restaurant_stats").select().decodeList()

    suspend fun categories(): List<Category> =
        db.from("categories").select { order("name", SortOrder.ASCENDING) }.decodeList()

    suspend fun menu(restaurantId: String): List<MenuItem> =
        db.from("menu_items").select {
            filter { eq("restaurant_id", restaurantId) }
            order("name", SortOrder.ASCENDING)
        }.decodeList()

    suspend fun searchDishes(q: String): List<MenuItem> =
        db.from("menu_items").select(Columns.raw("*, restaurants(name)")) {
            filter {
                ilike("name", "%$q%")
                eq("is_available", true)
            }
            limit(20)
        }.decodeList()

    suspend fun placeOrder(restaurantId: String, orderType: String, note: String, lines: List<CartLineDto>): String =
        db.rpc(
            "place_order",
            Json.encodeToJsonElement(PlaceOrderParams(restaurantId, orderType, note.ifBlank { null }, lines)).jsonObject
        ).decodeSingle<String>()

    suspend fun myOrders(): List<Order> =
        db.from("orders").select(Columns.raw(ORDER_COLS)) {
            filter { eq("customer_id", uid()) }
            order("created_at", SortOrder.DESCENDING)
        }.decodeList()

    suspend fun cancelOrder(orderId: String) {
        db.rpc("cancel_order", buildJsonObject { put("p_order_id", orderId) })
    }

    suspend fun reviewsFor(restaurantId: String): List<Review> =
        db.from("reviews").select {
            filter { eq("restaurant_id", restaurantId) }
            order("created_at", SortOrder.DESCENDING)
            limit(10)
        }.decodeList()

    suspend fun reviewForOrder(orderId: String): Review? =
        db.from("reviews").select { filter { eq("order_id", orderId) } }.decodeList<Review>().firstOrNull()

    suspend fun submitReview(order: Order, customerName: String, rating: Int, comment: String) {
        db.from("reviews").insert(
            NewReview(order.id, order.restaurantId, uid(), customerName, rating, comment.ifBlank { null })
        )
    }

    suspend fun report(restaurantId: String, reason: String) {
        db.from("reports").insert(NewReport(uid(), restaurantId, reason))
    }

    // ------------------------------------------------------ restaurant
    suspend fun myRestaurant(): Restaurant? =
        db.from("restaurants").select { filter { eq("owner_id", uid()) } }.decodeList<Restaurant>().firstOrNull()

    suspend fun createRestaurant(
        name: String,
        description: String,
        address: String,
        phone: String,
        email: String? = null,
        categoryId: String? = null,
        imageUrl: String? = null,
        coverImageUrl: String? = null,
        city: String? = null,
        district: String? = null,
        postalCode: String? = null,
        businessRegistrationNo: String? = null,
        minOrderAmount: Double = 0.0,
        estimatedPrepTimeMinutes: Int? = null,
        deliveryRadiusKm: Double? = null,
    ): Restaurant =
        db.from("restaurants").insert(
            NewRestaurant(
                ownerId = uid(),
                name = name,
                description = description.ifBlank { null },
                address = address.ifBlank { null },
                phone = phone.ifBlank { null },
                email = email?.ifBlank { null },
                categoryId = categoryId,
                imageUrl = imageUrl?.ifBlank { null },
                coverImageUrl = coverImageUrl?.ifBlank { null },
                city = city?.ifBlank { null },
                district = district?.ifBlank { null },
                postalCode = postalCode?.ifBlank { null },
                businessRegistrationNo = businessRegistrationNo?.ifBlank { null },
                minOrderAmount = minOrderAmount,
                estimatedPrepTimeMinutes = estimatedPrepTimeMinutes,
                deliveryRadiusKm = deliveryRadiusKm,
            )
        ) { select() }.decodeSingle<Restaurant>()

    suspend fun updateRestaurant(
        id: String,
        name: String,
        description: String,
        address: String,
        phone: String,
        email: String? = null,
        categoryId: String? = null,
        imageUrl: String? = null,
        coverImageUrl: String? = null,
        city: String? = null,
        district: String? = null,
        postalCode: String? = null,
        businessRegistrationNo: String? = null,
        minOrderAmount: Double = 0.0,
        estimatedPrepTimeMinutes: Int? = null,
        deliveryRadiusKm: Double? = null,
        isOpen: Boolean = true,
    ) {
        db.from("restaurants").update(buildJsonObject {
            put("name", name)
            put("description", description.ifBlank { null })
            put("address", address.ifBlank { null })
            put("phone", phone.ifBlank { null })
            put("email", email?.ifBlank { null })
            put("category_id", categoryId)
            put("image_url", imageUrl?.ifBlank { null })
            put("cover_image_url", coverImageUrl?.ifBlank { null })
            put("city", city?.ifBlank { null })
            put("district", district?.ifBlank { null })
            put("postal_code", postalCode?.ifBlank { null })
            put("business_registration_no", businessRegistrationNo?.ifBlank { null })
            put("min_order_amount", minOrderAmount)
            put("estimated_prep_time_minutes", estimatedPrepTimeMinutes)
            put("delivery_radius_km", deliveryRadiusKm)
            put("is_open", isOpen)
        }) { filter { eq("id", id) } }
    }

    suspend fun restaurantOrders(restaurantId: String): List<Order> =
        db.from("orders").select(Columns.raw(ORDER_COLS)) {
            filter { eq("restaurant_id", restaurantId) }
            order("created_at", SortOrder.DESCENDING)
            limit(200)
        }.decodeList()

    suspend fun setOrderStatus(orderId: String, status: String, minutes: Int? = null, reason: String? = null) {
        db.from("orders").update(buildJsonObject {
            put("status", status)
            if (minutes != null) put("estimated_minutes", minutes)
            if (reason != null) put("reject_reason", reason)
        }) { filter { eq("id", orderId) } }
    }

    suspend fun setOrderEta(orderId: String, minutes: Int) {
        db.from("orders").update(buildJsonObject { put("estimated_minutes", minutes) }) {
            filter { eq("id", orderId) }
        }
    }

    suspend fun saveMenuItem(
        id: String?,
        restaurantId: String,
        categoryId: String?,
        categoryName: String?,
        name: String,
        description: String,
        price: Double,
        portionSizes: List<PortionSize>,
        available: Boolean,
        imageUrl: String? = null
    ) {
        if (id == null) {
            db.from("menu_items").insert(
                NewMenuItem(
                    restaurantId = restaurantId,
                    categoryId = categoryId,
                    categoryName = categoryName?.ifBlank { null },
                    name = name,
                    description = description.ifBlank { null },
                    price = price,
                    portionSizes = portionSizes,
                    isAvailable = available,
                    imageUrl = imageUrl?.ifBlank { null }
                )
            )
        } else {
            db.from("menu_items").update(buildJsonObject {
                put("category_id", categoryId)
                put("category_name", categoryName?.ifBlank { null })
                put("name", name)
                put("description", description.ifBlank { null })
                put("price", price)
                put("portion_sizes", Json.encodeToJsonElement(portionSizes))
                put("is_available", available)
                if (imageUrl != null) put("image_url", imageUrl.ifBlank { null })
            }) { filter { eq("id", id) } }
        }
    }

    suspend fun setItemAvailable(id: String, available: Boolean) {
        db.from("menu_items").update(buildJsonObject { put("is_available", available) }) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteMenuItem(id: String) {
        db.from("menu_items").delete { filter { eq("id", id) } }
    }

    // ----------------------------------------------------------- admin
    suspend fun adminStats(): AdminStats = db.rpc("admin_stats").decodeSingle<AdminStats>()

    suspend fun allRestaurants(): List<Restaurant> =
        db.from("restaurants").select { order("name", SortOrder.ASCENDING) }.decodeList()

    suspend fun setRestaurantActive(id: String, active: Boolean) {
        db.from("restaurants").update(buildJsonObject { put("is_active", active) }) {
            filter { eq("id", id) }
        }
    }

    suspend fun approveRestaurant(id: String, approved: Boolean) {
        db.from("restaurants").update(buildJsonObject {
            put("approval_status", if (approved) "approved" else "rejected")
            put("is_active", approved)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun allProfiles(): List<Profile> =
        db.from("profiles").select { order("created_at", SortOrder.DESCENDING) }.decodeList()

    suspend fun setBlocked(id: String, blocked: Boolean) {
        db.from("profiles").update(buildJsonObject { put("is_blocked", blocked) }) {
            filter { eq("id", id) }
        }
    }

    suspend fun allOrders(): List<Order> =
        db.from("orders").select(Columns.raw(ORDER_COLS)) {
            order("created_at", SortOrder.DESCENDING)
            limit(100)
        }.decodeList()

    suspend fun addCategory(name: String) {
        db.from("categories").insert(buildJsonObject { put("name", name) })
    }

    suspend fun deleteCategory(id: String) {
        db.from("categories").delete { filter { eq("id", id) } }
    }

    suspend fun openReports(): List<Report> =
        db.from("reports").select(Columns.raw("*, restaurants(name)")) {
            filter { eq("status", "open") }
            order("created_at", SortOrder.DESCENDING)
        }.decodeList()

    suspend fun resolveReport(id: String) {
        db.from("reports").update(buildJsonObject { put("status", "resolved") }) {
            filter { eq("id", id) }
        }
    }

    // -------------------------------------------------------- realtime
    /**
     * Emits Unit every time a row in `orders` that the current user may see is inserted or updated.
     * Row Level Security makes sure customers only hear about their orders and
     * restaurants only about theirs. Collectors then simply re-load the list.
     */
    fun orderChanges(): Flow<Unit> = flow {
        val channel = supabase.channel("orders-${System.nanoTime()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "orders"
        }
        channel.subscribe(blockUntilSubscribed = true)
        try {
            changes.collect { emit(Unit) }
        } finally {
            withContext(NonCancellable) {
                runCatching { channel.unsubscribe() }
                runCatching { supabase.realtime.removeChannel(channel) }
            }
        }
    }
}
