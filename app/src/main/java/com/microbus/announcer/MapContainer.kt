package com.microbus.announcer

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout


class MapContainer : RelativeLayout {
    private var nestedScrollView: ConstraintLayout? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setScrollView(nestedScrollView: ConstraintLayout?) {
        this.nestedScrollView = nestedScrollView
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
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