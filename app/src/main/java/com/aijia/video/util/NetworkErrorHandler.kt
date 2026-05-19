package com.aijia.video.util

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 网络错误处理工具
 */
object NetworkErrorHandler {

    /**
     * 格式化网络错误信息
     */
    fun formatError(e: Exception): Exception {
        val message = e.message?.lowercase() ?: ""
        return when {
            message.contains("failed to connect") ||
            message.contains("unable to connect") ||
            message.contains("connection refused") ||
            message.contains("connection timed out") ||
            message.contains("connecttimeout") ||
            message.contains("sockettimeout") ||
            e is SocketTimeoutException ||
            e is ConnectException ||
            e is UnknownHostException -> {
                Exception("服务暂时不可用，我们正在紧急修复中。\n请稍后再试，感谢您的耐心！")
            }
            else -> e
        }
    }
}
