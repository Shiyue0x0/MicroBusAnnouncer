package com.microbus.announcer.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout


class Container : LinearLayout {
    private var nestedScrollView: ViewGroup? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    fun setScrollView(nestedScrollView: ViewGroup?) {
        this.nestedScrollView = nestedScrollView
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        MotionEvent.ACTION_SCROLL
        if (ev.action == MotionEvent.ACTION_UP) {
            nestedScrollView!!.requestDisallowInterceptTouchEvent(false)
        } else {
            nestedScrollView!!.requestDisallowInterceptTouchEvent(true)
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return true
    }
}