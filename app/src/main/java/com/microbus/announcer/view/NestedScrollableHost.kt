/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microbus.announcer.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import com.microbus.announcer.R
import kotlin.math.absoluteValue
import kotlin.math.sign
import androidx.core.content.withStyledAttributes
import androidx.core.view.size


/**
 * Layout to wrap a scrollable component inside a ViewPager2 or RecyclerView.
 * Provided as a solution to the problem where pages of ViewPager2 have nested
 * scrollable elements that scroll in the same direction as ViewPager2.
 * The scrollable element needs to be the immediate and only child of this host layout.
 *
 * This solution has limitations when using multiple levels of nested scrollable elements
 * (e.g. a horizontal RecyclerView in a vertical RecyclerView in a horizontal ViewPager2).
 */
class NestedScrollableHost : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        context.withStyledAttributes(attrs, R.styleable.NestedScrollableHost) {
            isChildHasSameDirection =
                getBoolean(R.styleable.NestedScrollableHost_sameDirectionWithParent, false)
            parentViewType = getInt(R.styleable.NestedScrollableHost_parentViewType, 0)
        }
    }

    private var parentViewType = 0
    private var isChildHasSameDirection = true
    private var touchSlop = 0
    private var initialX = 0f
    private var initialY = 0f

    // ⭐ 新增：记录触摸状态
    private var isDragging = false
    private var isHorizontalDrag = false
    private var isVerticalDrag = false

    // ⭐ 新增：是否已经确定了滑动方向
    private var isDirectionDetermined = false

    private var cachedParentViewPager: ViewPager2? = null
    private var cachedParentRecyclerView: RecyclerView? = null
    private var isParentDetected = false

    val parentViewPager: ViewPager2?
        get() {
            if (!isParentDetected) {
                detectParent()
            }
            return cachedParentViewPager
        }

    val parentRecyclerView: RecyclerView?
        get() {
            if (!isParentDetected) {
                detectParent()
            }
            return cachedParentRecyclerView
        }

    private val child: View? get() = getChildRecyclerView(this)

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    }

    private fun detectParent() {
        isParentDetected = true
        var v: View? = parent as? View
        while (v != null) {
            when {
                v is ViewPager2 -> {
                    cachedParentViewPager = v
                    return
                }
                v is RecyclerView -> {
                    cachedParentRecyclerView = v
                    return
                }
            }
            v = v.parent as? View
        }
    }

    private fun getParentOrientation(): Int {
        when (parentViewType) {
            1 -> return ORIENTATION_HORIZONTAL
            2 -> return 1
        }

        parentViewPager?.let { return it.orientation }
        parentRecyclerView?.let {
            val layoutManager = it.layoutManager
            if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
                return if (layoutManager.orientation == androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL) 0 else 1
            }
            return 1
        }
        return -1
    }

    private fun canChildScroll(orientation: Int, delta: Float): Boolean {
        val direction = -delta.sign.toInt()
        return when (orientation) {
            0 -> child?.canScrollHorizontally(direction) ?: false
            1 -> child?.canScrollVertically(direction) ?: false
            else -> false
        }
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        handleInterceptTouchEvent(e)
        return super.onInterceptTouchEvent(e)
    }

    private fun handleInterceptTouchEvent(e: MotionEvent) {
        val orientation = getParentOrientation()
        if (orientation == -1) {
            return
        }

        val childOrientation = if (isChildHasSameDirection) orientation else orientation xor 1
        if (!canChildScroll(childOrientation, -1f) && !canChildScroll(childOrientation, 1f)) {
            return
        }

        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                // ⭐ 重置所有状态
                initialX = e.x
                initialY = e.y
                isDragging = false
                isHorizontalDrag = false
                isVerticalDrag = false
                isDirectionDetermined = false
                parent.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = e.x - initialX
                val dy = e.y - initialY
                val absDx = dx.absoluteValue
                val absDy = dy.absoluteValue
                val isParentHorizontal = orientation == ORIENTATION_HORIZONTAL

                // ⭐ 如果还没有确定方向
                if (!isDirectionDetermined) {
                    // 检查是否超过了触摸阈值
                    if (absDx > touchSlop || absDy > touchSlop) {
                        isDragging = true
                        isDirectionDetermined = true

                        // ⭐ 判断滑动方向 - 使用原始位移，不缩放
                        if (absDx > absDy) {
                            // 水平滑动
                            isHorizontalDrag = true
                            isVerticalDrag = false

                            // ⭐ 水平滑动：检查子 RecyclerView 是否可以水平滚动
                            if (canChildScroll(0, dx)) {
                                // 子视图可以水平滚动，禁止父视图拦截
                                parent.requestDisallowInterceptTouchEvent(true)
                            } else {
                                // 子视图不能滚动（已到边界），允许父视图拦截
                                parent.requestDisallowInterceptTouchEvent(false)
                            }
                        } else {
                            // 垂直滑动
                            isHorizontalDrag = false
                            isVerticalDrag = true

                            // ⭐ 垂直滑动：让父视图处理
                            parent.requestDisallowInterceptTouchEvent(false)
                        }
                    }
                } else {
                    // ⭐ 方向已经确定，根据方向处理
                    if (isHorizontalDrag) {
                        // 水平拖动中
                        if (canChildScroll(0, dx)) {
                            // 子视图可以继续水平滚动，禁止父视图拦截
                            parent.requestDisallowInterceptTouchEvent(true)
                        } else {
                            // ⭐ 子视图已到边界，且继续向边界方向滑动
                            // 判断是否已经到达边界
                            if (dx > 0 && !child?.canScrollHorizontally(1)!!) {
                                // 向右滑动已到右边界
                                parent.requestDisallowInterceptTouchEvent(false)
                            } else if (dx < 0 && !child?.canScrollHorizontally(-1)!!) {
                                // 向左滑动已到左边界
                                parent.requestDisallowInterceptTouchEvent(false)
                            } else {
                                parent.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                    } else if (isVerticalDrag) {
                        // ⭐ 垂直拖动：始终交给父视图
                        parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // ⭐ 重置状态
                isDragging = false
                isHorizontalDrag = false
                isVerticalDrag = false
                isDirectionDetermined = false
                parent.requestDisallowInterceptTouchEvent(false)
            }
        }
    }

    fun getChildRecyclerView(view: View?): View? {
        val unvisited = ArrayList<View?>()
        unvisited.add(view)

        while (unvisited.isNotEmpty()) {
            val child = unvisited.removeAt(0)
            if (child is RecyclerView) {
                return child
            }
            if (child !is ViewGroup) {
                continue
            }
            val viewGroup = child
            for (i in 0 until viewGroup.size) {
                unvisited.add(viewGroup.getChildAt(i))
            }
        }
        return null
    }

    fun setParentViewType(type: Int) {
        parentViewType = type
    }
}