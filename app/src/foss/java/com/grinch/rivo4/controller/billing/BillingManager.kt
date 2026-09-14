package com.grinch.rivo4.controller.billing

import android.app.Activity
import android.content.Context
import com.grinch.rivo4.controller.util.PreferenceManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class TipProduct(
    val id: String,
    val title: String,
    val description: String,
    val defaultPrice: String,
    val formattedPrice: String,
    val iconType: String
)

class BillingManager(
    private val context: Context,
    private val preferenceManager: PreferenceManager
) {
    companion object {
        const val PRODUCT_COFFEE = "tip_coffee"
        const val PRODUCT_PIZZA = "tip_pizza"
        const val PRODUCT_ROCKET = "tip_rocket"

        val IS_BILLING_SUPPORTED = false
    }

    private val _products = MutableStateFlow<List<TipProduct>>(
        listOf(
            TipProduct(PRODUCT_COFFEE, "Coffee", "Fuel the next feature with caffeine", "$1.99", "$1.99", "coffee"),
            TipProduct(PRODUCT_PIZZA, "Pizza", "A warm meal to keep development moving", "$4.99", "$4.99", "pizza"),
            TipProduct(PRODUCT_ROCKET, "Rocket", "Major support for ongoing Rivo development", "$9.99", "$9.99", "rocket")
        )
    )
    val products: StateFlow<List<TipProduct>> = _products.asStateFlow()

    private val _purchaseSuccessEvent = MutableSharedFlow<Boolean>()
    val purchaseSuccessEvent: SharedFlow<Boolean> = _purchaseSuccessEvent.asSharedFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    fun startConnection() {}

    fun launchBillingFlow(activity: Activity, tipProduct: TipProduct): Boolean {
        return false
    }
}
