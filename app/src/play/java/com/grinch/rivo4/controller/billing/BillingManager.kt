package com.grinch.rivo4.controller.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.grinch.rivo4.controller.util.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TipProduct(
    val id: String,
    val title: String,
    val description: String,
    val defaultPrice: String,
    val formattedPrice: String,
    val iconType: String,
    val productDetails: ProductDetails? = null
)

class BillingManager(
    private val context: Context,
    private val preferenceManager: PreferenceManager
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val PRODUCT_COFFEE = "tip_coffee"
        const val PRODUCT_PIZZA = "tip_pizza"
        const val PRODUCT_ROCKET = "tip_rocket"

        val IS_BILLING_SUPPORTED = true
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _products = MutableStateFlow<List<TipProduct>>(defaultTipProducts())
    val products: StateFlow<List<TipProduct>> = _products.asStateFlow()

    private val _purchaseSuccessEvent = MutableSharedFlow<Boolean>()
    val purchaseSuccessEvent: SharedFlow<Boolean> = _purchaseSuccessEvent.asSharedFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        startConnection()
    }

    fun startConnection() {
        if (billingClient.isReady) {
            queryProducts()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client setup successfully connected.")
                    queryProducts()
                } else {
                    Log.w(TAG, "Billing setup finished with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected.")
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_COFFEE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PIZZA)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ROCKET)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val detailsMap = queryResult.associateBy { it.productId }
                val updated = defaultTipProducts().map { fallback ->
                    val details = detailsMap[fallback.id]
                    val price = details?.oneTimePurchaseOfferDetails?.formattedPrice ?: fallback.defaultPrice
                    val title = details?.title?.substringBefore(" (") ?: fallback.title
                    fallback.copy(
                        title = title,
                        formattedPrice = price,
                        productDetails = details
                    )
                }
                _products.value = updated
                Log.d(TAG, "Successfully loaded ${queryResult.size} tip products from Google Play.")
            } else {
                Log.w(TAG, "Failed to query products: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun launchBillingFlow(activity: Activity, tipProduct: TipProduct): Boolean {
        val details = tipProduct.productDetails
        if (details == null) {
            Log.w(TAG, "ProductDetails not available for ${tipProduct.id}, attempting to reconnect...")
            startConnection()
            return false
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _isPurchasing.value = true
        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _isPurchasing.value = false
            Log.e(TAG, "Failed to launch billing flow: ${result.responseCode} - ${result.debugMessage}")
            return false
        }
        return true
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isPurchasing.value = false
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled the purchase flow.")
        } else {
            Log.w(TAG, "Purchases updated error: ${billingResult.responseCode} - ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()

            billingClient.consumeAsync(consumeParams) { billingResult, _ ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase consumed successfully. Granting Supporter status.")
                    preferenceManager.setSupporter(true)
                    scope.launch {
                        _purchaseSuccessEvent.emit(true)
                    }
                } else {
                    Log.w(TAG, "Failed to consume purchase: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                }
            }
        }
    }

    private fun defaultTipProducts(): List<TipProduct> {
        return listOf(
            TipProduct(
                id = PRODUCT_COFFEE,
                title = "Coffee",
                description = "Fuel the next feature with caffeine",
                defaultPrice = "$1.99",
                formattedPrice = "$1.99",
                iconType = "coffee"
            ),
            TipProduct(
                id = PRODUCT_PIZZA,
                title = "Pizza",
                description = "A warm meal to keep development moving",
                defaultPrice = "$4.99",
                formattedPrice = "$4.99",
                iconType = "pizza"
            ),
            TipProduct(
                id = PRODUCT_ROCKET,
                title = "Rocket",
                description = "Major support for ongoing Rivo development",
                defaultPrice = "$9.99",
                formattedPrice = "$9.99",
                iconType = "rocket"
            )
        )
    }
}
