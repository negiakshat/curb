package com.example.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.ReceiveOfferingsCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SubscriptionPackageInfo(
    val id: String,
    val productId: String,
    val title: String,
    val priceString: String,
    val period: String,
    val billingDetail: String,
    val isBestValue: Boolean = false,
    val rawPackage: Package? = null
)

data class SubscriptionUiState(
    val isPro: Boolean = false,
    val isLoading: Boolean = false,
    val packages: List<SubscriptionPackageInfo> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isConfigured: Boolean = false
)

class SubscriptionService(private val context: Context) {

    companion object {
        private const val TAG = "CurbSubscription"
        const val ENTITLEMENT_PRO = "pro_access"

        const val PRODUCT_WEEKLY = "curb_pro_weekly"
        const val PRODUCT_MONTHLY = "curb_pro_monthly"
        const val PRODUCT_ANNUAL = "curb_pro_annual"

        // Fallback pricing packages if RevenueCat offerings are still loading, offline, or in demo mode
        val DEFAULT_PACKAGES = listOf(
            SubscriptionPackageInfo(
                id = "weekly",
                productId = PRODUCT_WEEKLY,
                title = "Weekly",
                priceString = "$2.99",
                period = "/ week",
                billingDetail = "Billed weekly • Cancel anytime",
                isBestValue = false
            ),
            SubscriptionPackageInfo(
                id = "monthly",
                productId = PRODUCT_MONTHLY,
                title = "Monthly",
                priceString = "$4.99",
                period = "/ month",
                billingDetail = "Billed monthly • Most flexible",
                isBestValue = false
            ),
            SubscriptionPackageInfo(
                id = "annual",
                productId = PRODUCT_ANNUAL,
                title = "Annual",
                priceString = "$29.99",
                period = "/ year",
                billingDetail = "$2.50/mo • Best Value",
                isBestValue = true
            )
        )

        /**
         * Validates whether a given API key is a usable RevenueCat Public API key
         * and not an empty, placeholder, or dummy key string.
         */
        fun isConfigurableKey(key: String?): Boolean {
            if (key.isNullOrBlank()) return false
            val trimmed = key.trim()
            val lower = trimmed.lowercase()
            if (lower == "revenuecat_public_api_key" ||
                lower.contains("placeholder") ||
                lower.contains("default_value") ||
                lower.contains("your_key") ||
                lower.contains("curb_pro") ||
                lower.contains("dummy") ||
                lower.contains("test")
            ) {
                return false
            }
            // RevenueCat Android public keys must start with 'goog_' or 'amzn_' and typically contain 30+ characters
            if (!trimmed.startsWith("goog_") && !trimmed.startsWith("amzn_")) {
                return false
            }
            return trimmed.length >= 24
        }
    }

    private val _subscriptionState = MutableStateFlow(
        SubscriptionUiState(
            packages = DEFAULT_PACKAGES,
            isLoading = false
        )
    )
    val subscriptionState: StateFlow<SubscriptionUiState> = _subscriptionState.asStateFlow()

    private var onProStatusChanged: ((Boolean) -> Unit)? = null

    fun initialize(onProStatusUpdated: (Boolean) -> Unit) {
        this.onProStatusChanged = onProStatusUpdated

        try {
            val apiKey = try {
                BuildConfig.REVENUECAT_PUBLIC_API_KEY
            } catch (e: Throwable) {
                ""
            }

            // Only attempt RevenueCat SDK configuration if a valid key is supplied
            if (!isConfigurableKey(apiKey)) {
                Log.d(TAG, "RevenueCat public API key is not configured or is a placeholder.")
                _subscriptionState.value = _subscriptionState.value.copy(
                    isConfigured = false,
                    packages = DEFAULT_PACKAGES,
                    isLoading = false
                )
                return
            }

            // Set custom log handler to gracefully catch and handle Billing unavailable logs on emulators/test environments
            Purchases.logHandler = object : com.revenuecat.purchases.LogHandler {
                override fun v(tag: String, msg: String) {
                    Log.v(TAG, msg)
                }

                override fun d(tag: String, msg: String) {
                    Log.d(TAG, msg)
                }

                override fun i(tag: String, msg: String) {
                    Log.i(TAG, msg)
                }

                override fun w(tag: String, msg: String) {
                    Log.w(TAG, msg)
                }

                override fun e(tag: String, msg: String, tr: Throwable?) {
                    if (msg.contains("BILLING_UNAVAILABLE") ||
                        msg.contains("Billing is not available") ||
                        msg.contains("PurchaseNotAllowedError") ||
                        msg.contains("API Key is not recognized") ||
                        msg.contains("The specified API Key is not recognized")
                    ) {
                        Log.d(TAG, "Store billing diagnostic: $msg")
                    } else if (tr != null) {
                        Log.e(TAG, msg, tr)
                    } else {
                        Log.e(TAG, msg)
                    }
                }
            }

            if (!Purchases.isConfigured) {
                Purchases.configure(
                    PurchasesConfiguration.Builder(context, apiKey)
                        .showInAppMessagesAutomatically(false)
                        .build()
                )
                Log.d(TAG, "RevenueCat initialized successfully with anonymous app user ID.")
            }

            _subscriptionState.value = _subscriptionState.value.copy(isConfigured = true)

            // Setup real-time customer info listener
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                handleCustomerInfo(customerInfo)
            }

            // Fetch initial customer info and offerings
            refreshCustomerInfo()
            fetchOfferings()

        } catch (e: Throwable) {
            Log.w(TAG, "RevenueCat initialization handled: ${e.message}")
            _subscriptionState.value = _subscriptionState.value.copy(
                isConfigured = false,
                packages = DEFAULT_PACKAGES
            )
        }
    }

    fun refreshCustomerInfo() {
        if (!Purchases.isConfigured) return

        try {
            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    handleCustomerInfo(customerInfo)
                }

                override fun onError(error: PurchasesError) {
                    Log.d(TAG, "Notice on customer info: ${error.message}")
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "getCustomerInfo caught: ${e.message}")
        }
    }

    private fun handleCustomerInfo(customerInfo: CustomerInfo) {
        val hasPro = customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true
        _subscriptionState.value = _subscriptionState.value.copy(
            isPro = hasPro
        )
        onProStatusChanged?.invoke(hasPro)
    }

    fun fetchOfferings() {
        if (!Purchases.isConfigured) return

        try {
            Purchases.sharedInstance.getOfferings(object : ReceiveOfferingsCallback {
                override fun onReceived(offerings: com.revenuecat.purchases.Offerings) {
                    val currentOffering = offerings.current
                    if (currentOffering != null && currentOffering.availablePackages.isNotEmpty()) {
                        val mappedPackages = currentOffering.availablePackages.map { pkg ->
                            val storeProduct = pkg.product
                            val isAnnual = pkg.identifier.contains("annual", ignoreCase = true) ||
                                    storeProduct.id.contains("annual", ignoreCase = true) ||
                                    pkg.packageType == com.revenuecat.purchases.PackageType.ANNUAL
                            val isWeekly = pkg.identifier.contains("weekly", ignoreCase = true) ||
                                    storeProduct.id.contains("weekly", ignoreCase = true) ||
                                    pkg.packageType == com.revenuecat.purchases.PackageType.WEEKLY

                            val title = when {
                                isAnnual -> "Annual"
                                isWeekly -> "Weekly"
                                else -> "Monthly"
                            }
                            val period = when {
                                isAnnual -> "/ year"
                                isWeekly -> "/ week"
                                else -> "/ month"
                            }
                            val billing = when {
                                isAnnual -> "$2.50/mo • Best Value"
                                isWeekly -> "Billed weekly • Cancel anytime"
                                else -> "Billed monthly • Most flexible"
                            }

                            SubscriptionPackageInfo(
                                id = pkg.identifier,
                                productId = storeProduct.id,
                                title = title,
                                priceString = storeProduct.price.formatted,
                                period = period,
                                billingDetail = billing,
                                isBestValue = isAnnual,
                                rawPackage = pkg
                            )
                        }

                        if (mappedPackages.isNotEmpty()) {
                            val sorted = mappedPackages.sortedBy {
                                when {
                                    it.isBestValue -> 3
                                    it.title == "Monthly" -> 2
                                    else -> 1
                                }
                            }
                            _subscriptionState.value = _subscriptionState.value.copy(
                                packages = sorted
                            )
                        }
                    }
                }

                override fun onError(error: PurchasesError) {
                    Log.d(TAG, "Offerings notice: ${error.message}")
                    // Keep default package fallback active
                    _subscriptionState.value = _subscriptionState.value.copy(
                        packages = DEFAULT_PACKAGES
                    )
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "fetchOfferings caught: ${e.message}")
        }
    }

    fun purchasePackage(
        activity: Activity,
        packageInfo: SubscriptionPackageInfo,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Purchases.isConfigured) {
            val msg = "Store billing is currently unavailable on this device. If you are demoing the app, please use promo code CURB26X."
            _subscriptionState.value = _subscriptionState.value.copy(
                isLoading = false,
                errorMessage = msg
            )
            onError(msg)
            return
        }

        val rawPkg = packageInfo.rawPackage
        if (rawPkg == null) {
            val msg = "This package is currently unavailable from the store. Please try again."
            _subscriptionState.value = _subscriptionState.value.copy(
                isLoading = false,
                errorMessage = msg
            )
            onError(msg)
            return
        }

        _subscriptionState.value = _subscriptionState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        try {
            val params = PurchaseParams.Builder(activity, rawPkg).build()

            Purchases.sharedInstance.purchase(
                params,
                object : PurchaseCallback {
                    override fun onCompleted(storeTransaction: StoreTransaction, customerInfo: CustomerInfo) {
                        val isProActive = customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true
                        _subscriptionState.value = _subscriptionState.value.copy(
                            isLoading = false,
                            isPro = isProActive,
                            successMessage = if (isProActive) "Welcome to Curb Pro!" else null
                        )
                        if (isProActive) {
                            onProStatusChanged?.invoke(true)
                            onSuccess()
                        } else {
                            val errorMsg = "Purchase completed, but the 'pro_access' entitlement was not active."
                            _subscriptionState.value = _subscriptionState.value.copy(
                                errorMessage = errorMsg
                            )
                            onError(errorMsg)
                        }
                    }

                    override fun onError(error: PurchasesError, userCancelled: Boolean) {
                        _subscriptionState.value = _subscriptionState.value.copy(
                            isLoading = false
                        )
                        if (userCancelled) {
                            Log.d(TAG, "Purchase cancelled by user.")
                        } else {
                            val userFriendlyMessage = when (error.code) {
                                PurchasesErrorCode.PurchaseCancelledError -> "Purchase was cancelled."
                                PurchasesErrorCode.PaymentPendingError -> "Payment is pending approval."
                                PurchasesErrorCode.ProductAlreadyPurchasedError -> "You already own this subscription. Try 'Restore Purchases'."
                                PurchasesErrorCode.PurchaseNotAllowedError,
                                PurchasesErrorCode.StoreProblemError -> "In-app billing is unavailable on this device."
                                PurchasesErrorCode.NetworkError -> "Network connection error. Please check your connection."
                                else -> error.message
                            }
                            _subscriptionState.value = _subscriptionState.value.copy(
                                errorMessage = userFriendlyMessage
                            )
                            onError(userFriendlyMessage)
                        }
                    }
                }
            )
        } catch (e: Throwable) {
            _subscriptionState.value = _subscriptionState.value.copy(
                isLoading = false,
                errorMessage = e.message
            )
            onError(e.message ?: "Failed to initiate purchase.")
        }
    }

    fun restorePurchases(
        onSuccess: (Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!Purchases.isConfigured) {
            val msg = "Store service is unavailable to restore purchases."
            _subscriptionState.value = _subscriptionState.value.copy(
                isLoading = false,
                errorMessage = msg
            )
            onError(msg)
            return
        }

        _subscriptionState.value = _subscriptionState.value.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        try {
            Purchases.sharedInstance.restorePurchases(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val isProActive = customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true
                    _subscriptionState.value = _subscriptionState.value.copy(
                        isLoading = false,
                        isPro = isProActive,
                        successMessage = if (isProActive) "Your Curb Pro subscription has been restored." else null,
                        errorMessage = if (!isProActive) "No active Curb Pro purchase was found." else null
                    )
                    if (isProActive) {
                        onProStatusChanged?.invoke(true)
                        onSuccess(true)
                    } else {
                        onSuccess(false)
                    }
                }

                override fun onError(error: PurchasesError) {
                    _subscriptionState.value = _subscriptionState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                    onError(error.message)
                }
            })
        } catch (e: Throwable) {
            _subscriptionState.value = _subscriptionState.value.copy(
                isLoading = false,
                errorMessage = e.message
            )
            onError(e.message ?: "Failed to restore purchases.")
        }
    }

    fun clearMessages() {
        _subscriptionState.value = _subscriptionState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
