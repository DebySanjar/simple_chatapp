package com.samurai.chatme.util

import android.os.Handler
import android.os.Looper
import android.widget.TextView

class TypingAnimationUtil(
    private val textView: TextView,
    private val text: String,
    private val delay: Long = 150L
) {
    private var handler = Handler(Looper.getMainLooper())
    private var index = 0
    private var isStopped = false

    private val runnable = object : Runnable {
        override fun run() {
            if (!isStopped) {
                if (index <= text.length) {
                    textView.text = text.substring(0, index)
                    index++
                    handler.postDelayed(this, delay)
                } else {

                    index = 0
                    handler.postDelayed(this, delay)
                }
            }
        }
    }

    fun start() {
        handler.post(runnable)
    }

    fun stop() {
        isStopped = true
        handler.removeCallbacks(runnable)
    }
}
