package edu.skku.cs.autosen

import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver

class MyReceiver(handler: Handler) : ResultReceiver(handler) {
    private var receiver: Receiver? = null

    fun setReceiver(receiver1: Receiver) {
        this.receiver = receiver1
    }
    interface Receiver {
        fun onReceiverResult(resultCode: Int, resultData: Bundle)
    }
    override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
        receiver!!.onReceiverResult(resultCode, resultData)
    }

}