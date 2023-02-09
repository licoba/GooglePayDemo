package com.example.googlepaydemo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.android.billingclient.api.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import kotlinx.coroutines.Dispatchers


/**
 *  参考链接：https://developer.android.com/google/play/billing/integrate?hl=zh-cn
 */
class PayActivity : AppCompatActivity() {

    private lateinit var btnInitPay: Button
    private lateinit var btnConnect: Button
    private lateinit var btnQuery: Button

    // 文档：https://developer.android.com/google/play/billing/integrate?hl=zh-cn
    private lateinit var billingClient: BillingClient
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)
        DialogX.onlyOnePopTip = true
        context = this@PayActivity
        btnInitPay = findViewById(R.id.btn_init_google_pay)
        btnConnect = findViewById(R.id.btn_connect)
        btnQuery = findViewById(R.id.btn_query)
        btnInitPay.setOnClickListener { initGooglePay() }
        btnConnect.setOnClickListener { connectGooglePlay() }
        btnQuery.setOnClickListener { queryProducts() }
    }


    private fun initGooglePay() {
        PopTip.show("初始化billingClient成功").iconSuccess()
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }

    private fun connectGooglePlay() {
        LogUtils.d("连接到Google Play")
        try {
            billingClient.startConnection(connListener)
        } catch (e: Exception) {
            PopTip.show("请先初始化").iconError()
        }
    }

    private fun queryProducts() {
        LogUtils.d("查询商品列表")
        val queryProductDetailsParams =
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PayProductSku.Fish5.productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PayProductSku.Fish10.productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PayProductSku.Fish15.productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()

        try {
            WaitDialog.show("加载中...")
            billingClient.queryProductDetailsAsync(queryProductDetailsParams, queryListener)
        } catch (e: Exception) {
            WaitDialog.dismiss()
            PopTip.show("请先初始化").iconError()
        }
    }


    private val connListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                LogUtils.d("连接到Google Play成功，可以查询商品列表了")
                PopTip.show("连接到Google Play成功\n可以查询商品列表了").iconSuccess()
            } else {
                PopTip.show("连接结果：$billingResult").iconError()
            }
        }

        override fun onBillingServiceDisconnected() {
            // Try to restart the connection on the next request to
            // Google Play by calling the startConnection() method.
            PopTip.show("onBillingServiceDisconnected")
        }
    }


    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            LogUtils.e(billingResult.toString(), "购买结果")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    LogUtils.e(purchase.toString(), "Google付款成功")
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                PopTip.show("支付取消")
            } else {
                PopTip.show("支付失败")
            }
        }


    // 商品查询的监听列表
    private val queryListener =
        ProductDetailsResponseListener { billingResult, productDetailsList ->
            WaitDialog.dismiss()
            if (productDetailsList.isNotEmpty())
                PopTip.show("查询成功，请在logcat查看日志打印").iconSuccess()
            else
                PopTip.show("查询失败，请检查是否连接到了Google Play").iconError()
            LogUtils.e("查询成功，谷歌Google的SKU列表 ： \n$productDetailsList")
        }


}