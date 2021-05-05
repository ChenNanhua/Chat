package com.example.chat.chatUtil

import android.util.Log

object LogUtil {
    private const val VERBOSE = 1
    private const val DEBUG = 2
    private const val INFO = 3
    private const val WARN = 4
    private const val ERROR = 5
    private const val LEVEL = VERBOSE   //TODO 不想打印即改为ERROR
    fun v(tag: String, msg: String) {
        if (LEVEL <= VERBOSE) Log.v(tag, msg)
    }

    fun d(tag: String, msg: String) {
        if (LEVEL <= DEBUG) Log.w(tag, msg)
    }

    fun i(tag: String, msg: String) {
        if (LEVEL <= INFO) Log.i(tag, msg)
    }

    fun w(tag: String, msg: String) {
        if (LEVEL <= WARN) Log.w(tag, msg)
    }

    fun e(tag: String, msg: String) {
        if (LEVEL <= ERROR) Log.e(tag, msg)
    }
}