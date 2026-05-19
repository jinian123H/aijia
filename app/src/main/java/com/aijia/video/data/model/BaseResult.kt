package com.aijia.video.data.model

/**
 * 通用接口返回基类（兼容旧接口结构）
 */
open class BaseResult<T> {
    open val code: Int = -1
    open val msg: String = ""
    open val data: T? = null

    fun isSuccessful(): Boolean = code == 1 || code == 200
}
