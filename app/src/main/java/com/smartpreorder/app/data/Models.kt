package com.smartpreorder.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NameOnly(val name: String)

@Serializable
data class Profile(
    val id: String,
    @SerialName("full_name") val fullName: String = "",
    val phone: String? = null,
    val role: String = "customer",
    @SerialName("is_blocked") val isBlocked: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Restaurant(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val city: String? = null,
    val district: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("business_registration_no") val businessRegistrationNo: String? = null,
    @SerialName("min_order_amount") val minOrderAmount: Double = 0.0,
    @SerialName("estimated_prep_time_minutes") val estimatedPrepTimeMinutes: Int? = null,
    @SerialName("delivery_radius_km") val deliveryRadiusKm: Double? = null,
    @SerialName("approval_status") val approvalStatus: String = "pending",
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("approved_at") val approvedAt: String? = null,
    @SerialName("is_open") val isOpen: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class RestaurantStat(
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("avg_rating") val avgRating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
)

@Serializable
data class Category(val id: String, val name: String)

@Serializable
data class MenuItem(
    val id: String,
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("category_id") val categoryId: String? = null,
    val name: String,
    val description: String? = null,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = true,
    val restaurants: NameOnly? = null, // filled only by the food-search query
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("unit_price") val unitPrice: Double,
    val quantity: Int,
)

@Serializable
data class Order(
    val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("order_type") val orderType: String,
    val status: String,
    val total: Double = 0.0,
    val note: String? = null,
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("customer_phone") val customerPhone: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("order_items") val items: List<OrderItem> = emptyList(),
    val restaurants: NameOnly? = null,
)

@Serializable
data class Review(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("customer_name") val customerName: String? = null,
    val rating: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class Report(
    val id: String,
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("restaurant_id") val restaurantId: String? = null,
    val reason: String,
    val status: String = "open",
    @SerialName("created_at") val createdAt: String? = null,
    val restaurants: NameOnly? = null,
)

@Serializable
data class AdminStats(
    val customers: Int = 0,
    val restaurants: Int = 0,
    val orders: Int = 0,
    val revenue: Double = 0.0,
    @SerialName("by_status") val byStatus: Map<String, Int> = emptyMap(),
)

// ---------- request bodies ----------
@Serializable
data class NewRestaurant(
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("cover_image_url") val coverImageUrl: String? = null,
    val city: String? = null,
    val district: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    @SerialName("business_registration_no") val businessRegistrationNo: String? = null,
    @SerialName("min_order_amount") val minOrderAmount: Double = 0.0,
    @SerialName("estimated_prep_time_minutes") val estimatedPrepTimeMinutes: Int? = null,
    @SerialName("delivery_radius_km") val deliveryRadiusKm: Double? = null,
    @SerialName("approval_status") val approvalStatus: String = "pending",
    @SerialName("is_active") val isActive: Boolean = false,
)

@Serializable
data class NewMenuItem(
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("category_id") val categoryId: String?,
    val name: String,
    val description: String?,
    val price: Double,
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("image_url") val imageUrl: String? = null,
)

@Serializable
data class NewReview(
    @SerialName("order_id") val orderId: String,
    @SerialName("restaurant_id") val restaurantId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_name") val customerName: String,
    val rating: Int,
    val comment: String?,
)

@Serializable
data class NewReport(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("restaurant_id") val restaurantId: String,
    val reason: String,
)

@Serializable
data class CartLineDto(
    @SerialName("menu_item_id") val menuItemId: String,
    val quantity: Int,
)

@Serializable
data class PlaceOrderParams(
    @SerialName("p_restaurant_id") val restaurantId: String,
    @SerialName("p_order_type") val orderType: String,
    @SerialName("p_note") val note: String?,
    @SerialName("p_items") val items: List<CartLineDto>,
)
