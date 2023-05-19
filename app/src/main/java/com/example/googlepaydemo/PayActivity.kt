package com.example.googlepaydemo

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.TextView
import com.android.billingclient.api.*
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.ToastUtils.MODE.DARK
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


/**
 *  参考链接：https://developer.android.com/google/play/billing/integrate?hl=zh-cn
 */
class PayActivity : AppCompatActivity() {

    private lateinit var btnInitPay: Button
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var btnQuery: Button
    private lateinit var btnStartPay: Button
    private lateinit var tvCurProduct: TextView
    private lateinit var mToast: ToastUtils


    // 文档：https://developer.android.com/google/play/billing/integrate?hl=zh-cn
    private lateinit var billingClient: BillingClient
    private lateinit var context: Context
    private var curIndex = -1
    private var mProductList = mutableListOf<ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pay)
        DialogX.onlyOnePopTip = true
        context = this@PayActivity
        mToast = ToastUtils.make().setMode(DARK).setGravity(Gravity.CENTER, 0, 100)
        btnInitPay = findViewById(R.id.btn_init_google_pay)
        btnConnect = findViewById(R.id.btn_connect)
        btnDisconnect = findViewById(R.id.btn_end_connect)
        btnQuery = findViewById(R.id.btn_query)
        btnStartPay = findViewById(R.id.btn_start_pay)
        tvCurProduct = findViewById(R.id.tv_cur_product)
        btnInitPay.setOnClickListener { initGooglePay() }
        btnConnect.setOnClickListener { connectGooglePlay() }
        btnQuery.setOnClickListener { queryProducts() }
        btnStartPay.setOnClickListener { startPay() }
        btnDisconnect.setOnClickListener { endConnect() }
    }

    private fun endConnect() {
        PopTip.show("结束连接")
        billingClient.endConnection()
    }

    private fun startPay() {
        PopTip.show("发起支付")
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(mProductList[curIndex])
                    .build()
            )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val billingResult = billingClient.launchBillingFlow(this@PayActivity, billingFlowParams)
        // 支付结果在 purchasesUpdatedListener 的回调里查看
        LogUtils.e("billingResult 支付结果 $billingResult")
    }

    private fun initGooglePay() {
        PopTip.show("初始化billingClient成功").iconSuccess()
        BillingClientUtil.setPurchasesUpdatedListener(purchasesUpdatedListener)
        BillingClientUtil.initGooglePay()
        billingClient = BillingClientUtil.getClient()
//        billingClient = BillingClient.newBuilder(context)
//            .setListener(purchasesUpdatedListener)
//            .enablePendingPurchases()
//            .build()
    }

    private fun connectGooglePlay() {
        LogUtils.d("开始连接到Google Play")
        try {
            BillingClientUtil.setConnListener(connListener)
            BillingClientUtil.startConnect()
//            billingClient.startConnection(connListener)
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
            BillingClientUtil.setQueryListener(queryListener)
            BillingClientUtil.startQuery(queryProductDetailsParams)
//            billingClient.queryProductDetailsAsync(queryProductDetailsParams, queryListener)
        } catch (e: Exception) {
            WaitDialog.dismiss()
            PopTip.show("请先初始化").iconError()
        }
    }


    private var connListener: BillingClientStateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                MainScope().launch {
                    btnDisconnect.isEnabled = true
                    LogUtils.d("连接Google Play成功，可以查询商品列表了")
                    PopTip.show("连接到Google Play成功\n可以查询商品列表了").iconSuccess()
                }
            } else {
                PopTip.show("连接结果：$billingResult").iconError()
            }
        }

        override fun onBillingServiceDisconnected() {
            PopTip.show("onBillingServiceDisconnected")
        }
    }


    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            LogUtils.e(billingResult.toString(), "购买结果")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    mToast.show("购买成功！商品数[${purchase.products.size}]:\n${purchase.products}")
                    LogUtils.e(purchase.toString(), "Google付款成功")
                    consumePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                PopTip.show("支付取消").iconWarning()
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                mToast.show("已经购买了此内容，只能购买一次")
                purchases?.forEach {
                    consumePurchase(it)
                }
            } else {
                PopTip.show("支付失败").iconError()
            }
        }

    private fun consumePurchase(purchase: Purchase) {
        GlobalScope.launch {
            val consumeParams =
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            billingClient.consumePurchase(consumeParams)
            LogUtils.d("消费了商品 ${purchase}，下次可以继续购买")
        }
    }


    // 商品查询的监听列表
    private val queryListener =
        ProductDetailsResponseListener { billingResult, productDetailsList ->
            WaitDialog.dismiss()
            if (productDetailsList.isNotEmpty())
                PopTip.show("查询成功，商品总数：${productDetailsList.size} \n请在logcat查看日志打印").iconSuccess()
            else
                PopTip.show("查询失败，请检查是否连接到了Google Play").iconError()
            LogUtils.e("查询成功，谷歌Google的SKU列表 ： \n$productDetailsList")
            mProductList.clear()
            mProductList.addAll(productDetailsList)
            if (productDetailsList.isEmpty()) return@ProductDetailsResponseListener
            val list =productDetailsList.map { "${it.name} • ${it.oneTimePurchaseOfferDetails?.formattedPrice}" }
//                productDetailsList.map { "${it.productId} • ${it.name} • ${it.oneTimePurchaseOfferDetails?.formattedPrice}" }

            BottomMenu.show(list)
                .setOnMenuItemClickListener { dialog, text, index ->
                    PopTip.show("选择了 ${text}")
                    curIndex = index
                    tvCurProduct.text = text
                    btnStartPay.isEnabled = true
                    false
                }

        }

    override fun onDestroy() {
        BillingClientUtil.clearListener()
        super.onDestroy()
    }


}