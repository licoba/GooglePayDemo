package com.example.googlepaydemo

import android.app.Application
import android.content.Context
import com.android.billingclient.api.*
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.PopTip
import com.kongzue.dialogx.dialogs.WaitDialog
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * @author licoba
 * @date 2023/2/9 20:31
 * @email licoba@qq.com
 * @desc BillingClientUtil 提供billingClient的单例对象，防止内存泄漏
 */


object BillingClientUtil {


    private var billingClient: BillingClient? = null
    private var mPurchasesUpdatedListener: PurchasesUpdatedListener? = null // 提供给暴露给外部调用的Listener
    private var mConnListener: BillingClientStateListener? = null // 提供给暴露给外部调用的Listener
    private var mQueryListener: ProductDetailsResponseListener? = null // 提供给暴露给外部调用的Listener

    /**
     * 提供billingClient的单例
     * @return BillingClient?
     */
    fun getClient(): BillingClient {
        return billingClient!!
    }


    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            mPurchasesUpdatedListener?.onPurchasesUpdated(billingResult, purchases)
        }


    private val connListener: BillingClientStateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            mConnListener?.onBillingSetupFinished(billingResult)
        }

        override fun onBillingServiceDisconnected() {
            mConnListener?.onBillingServiceDisconnected()
        }
    }


    // 商品查询的监听列表
    private val queryListener =
        ProductDetailsResponseListener { billingResult, productDetailsList ->
            mQueryListener?.onProductDetailsResponse(billingResult, productDetailsList)
        }


    // region 外部接口暴露

    fun setPurchasesUpdatedListener(listener: PurchasesUpdatedListener) {
        mPurchasesUpdatedListener = listener
    }

    fun setConnListener(listener: BillingClientStateListener?) {
        mConnListener = listener
    }

    fun setQueryListener(listener: ProductDetailsResponseListener?) {
        mQueryListener = listener
    }


    /**
     * 初始化Google Pay
     */
    fun initGooglePay() {
        if (billingClient != null) return
        billingClient = BillingClient.newBuilder(MyApplication.context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
    }


    /**
     * 建立连接
     */
    fun startConnect() {
        billingClient!!.startConnection(connListener)
    }


    /**
     *
     * @param queryProductDetailsParams QueryProductDetailsParams
     * 查询商品列表
     */
    fun startQuery(params: QueryProductDetailsParams) {
        billingClient?.queryProductDetailsAsync(params, queryListener)
    }


    /**
     * 清除所有监听器
     */
    fun clearListener() {
        mConnListener = null
        mPurchasesUpdatedListener = null
        mQueryListener = null
    }




    // endregion
}