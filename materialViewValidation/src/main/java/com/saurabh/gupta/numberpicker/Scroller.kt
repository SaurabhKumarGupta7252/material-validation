/*
 * Copyright (C) 2006 The Android Open Source Project
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
package com.saurabh.gupta.numberpicker

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.view.ViewConfiguration
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import kotlin.math.exp
import kotlin.math.sign

/**
 *
 * This class encapsulates scrolling. You can use scrollers ([Scroller]
 * to collect the data you need to produce a scrolling animationfor
 * example, in response to a fling gesture. Scrollers track scroll offsets
 * for you over time, but they don't automatically apply those positions
 * to your view. It's your responsibility to get and apply new coordinates
 * at a rate that will make the scrolling animation look smooth.
 *
 *
 * Here is a simple example:
 *
 * <pre> private Scroller mScroller = new Scroller(context);
 * ...
 * public void zoomIn() {
 * // Revert any animation currently in progress
 * mScroller.forceFinished(true);
 * // Start scrolling by providing a starting point and
 * // the distance to travel
 * mScroller.startScroll(0, 0, 100, 0);
 * // Invalidate to request a redraw
 * invalidate();
 * }</pre>
 *
 *
 * To track the changing positions of the x/y coordinates, use
 * [.computeScrollOffset]. The method returns a boolean to indicate
 * whether the scroller is finished. If it isn't, it means that a fling or
 * programmatic pan operation is still in progress. You can use this method to
 * find the current offsets of the x and y coordinates, for example:
 *
 * <pre>if (mScroller.computeScrollOffset()) {
 * // Get current x and y positions
 * int currX = mScroller.getCurrX();
 * int currY = mScroller.getCurrY();
 * ...
 * }</pre>
 */
class Scroller @JvmOverloads constructor(
    context: Context, interpolator: Interpolator? = null, flywheel: Boolean =
        context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.HONEYCOMB
) {
    private var mInterpolator: Interpolator? = null
    private var mMode = 0

    /**
     * Returns the start X offset in the scroll.
     *
     * @return The start X offset as an absolute distance from the origin.
     */
    var startX = 0
        private set

    /**
     * Returns the start Y offset in the scroll.
     *
     * @return The start Y offset as an absolute distance from the origin.
     */
    var startY = 0
        private set
    private var mFinalX = 0
    private var mFinalY = 0
    private var mMinX = 0
    private var mMaxX = 0
    private var mMinY = 0
    private var mMaxY = 0

    /**
     * Returns the current X offset in the scroll.
     *
     * @return The new X offset as an absolute distance from the origin.
     */
    var currX = 0
        private set

    /**
     * Returns the current Y offset in the scroll.
     *
     * @return The new Y offset as an absolute distance from the origin.
     */
    var currY = 0
        private set
    private var mStartTime: Long = 0

    /**
     * Returns how long the scroll event will take, in milliseconds.
     *
     * @return The duration of the scroll in milliseconds.
     */
    var duration = 0
        private set
    private var mDurationReciprocal = 0f
    private var mDeltaX = 0f
    private var mDeltaY = 0f

    /**
     *
     * Returns whether the scroller has finished scrolling.
     *
     * @return True if the scroller has finished scrolling, false otherwise.
     */
    var isFinished = true
        private set
    private val mFlywheel: Boolean
    private var mVelocity = 0f
    private var mCurrVelocity = 0f
    private var mDistance = 0
    private var mFlingFriction = ViewConfiguration.getScrollFriction()
    private var mDeceleration: Float
    private val mPpi: Float

    // A context-specific coefficient adjusted to physical values.
    private val mPhysicalCoeff: Float
    /**
     * Create a Scroller with the specified interpolator. If the interpolator is
     * null, the default (viscous) interpolator will be used. Specify whether or
     * not to support progressive "flywheel" behavior in flinging.
     */
    /**
     * Create a Scroller with the specified interpolator. If the interpolator is
     * null, the default (viscous) interpolator will be used. "Flywheel" behavior will
     * be in effect for apps targeting Honeycomb or newer.
     */
    /**
     * Create a Scroller with the default duration and interpolator.
     */
    init {
        mInterpolator = interpolator ?: ViscousFluidInterpolator()
        mPpi = context.resources.displayMetrics.density * 160.0f
        mDeceleration = computeDeceleration(ViewConfiguration.getScrollFriction())
        mFlywheel = flywheel
        mPhysicalCoeff = computeDeceleration(0.84f) // look and feel tuning
    }

    /**
     * The amount of friction applied to flings. The default value
     * is [ViewConfiguration.getScrollFriction].
     *
     * @param friction A scalar dimension-less value representing the coefficient of
     * friction.
     */
    fun setFriction(friction: Float) {
        mDeceleration = computeDeceleration(friction)
        mFlingFriction = friction
    }

    private fun computeDeceleration(friction: Float): Float {
        return (SensorManager.GRAVITY_EARTH // g (m/s^2)
                * 39.37f // inch/meter
                * mPpi // pixels per inch
                * friction)
    }

    /**
     * Force the finished field to a particular value.
     *
     * @param finished The new finished value.
     */
    fun forceFinished(finished: Boolean) {
        isFinished = finished
    }

    /**
     * Returns the current velocity.
     *
     * @return The original velocity less the deceleration. Result may be
     * negative.
     */
    val currVelocity: Float
        get() = if (mMode == FLING_MODE) mCurrVelocity else mVelocity - mDeceleration * timePassed() / 2000.0f
    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final X offset as an absolute distance from the origin.
     */
    /**
     * Sets the final position (X) for this scroller.
     *
     * @param newX The new X offset as an absolute distance from the origin.
     * @see .extendDuration
     * @see .setFinalY
     */
    var finalX: Int
        get() = mFinalX
        set(newX) {
            mFinalX = newX
            mDeltaX = (mFinalX - startX).toFloat()
            isFinished = false
        }
    /**
     * Returns where the scroll will end. Valid only for "fling" scrolls.
     *
     * @return The final Y offset as an absolute distance from the origin.
     */
    /**
     * Sets the final position (Y) for this scroller.
     *
     * @param newY The new Y offset as an absolute distance from the origin.
     * @see .extendDuration
     * @see .setFinalX
     */
    var finalY: Int
        get() = mFinalY
        set(newY) {
            mFinalY = newY
            mDeltaY = (mFinalY - startY).toFloat()
            isFinished = false
        }

    /**
     * Call this when you want to know the new location.  If it returns true,
     * the animation is not yet finished.
     */
    fun computeScrollOffset(): Boolean {
        if (isFinished) {
            return false
        }
        val timePassed = (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()
        if (timePassed < duration) {
            when (mMode) {
                SCROLL_MODE -> {
                    val x = mInterpolator!!.getInterpolation(timePassed * mDurationReciprocal)
                    currX = startX + Math.round(x * mDeltaX)
                    currY = startY + Math.round(x * mDeltaY)
                }
                FLING_MODE -> {
                    val t = timePassed.toFloat() / duration
                    val index = (NB_SAMPLES * t).toInt()
                    var distanceCoef = 1f
                    var velocityCoef = 0f
                    if (index < NB_SAMPLES) {
                        val t_inf = index.toFloat() / NB_SAMPLES
                        val t_sup = (index + 1).toFloat() / NB_SAMPLES
                        val d_inf = SPLINE_POSITION[index]
                        val d_sup = SPLINE_POSITION[index + 1]
                        velocityCoef = (d_sup - d_inf) / (t_sup - t_inf)
                        distanceCoef = d_inf + (t - t_inf) * velocityCoef
                    }
                    mCurrVelocity = velocityCoef * mDistance / duration * 1000.0f
                    currX = startX + Math.round(distanceCoef * (mFinalX - startX))
                    // Pin to mMinX <= mCurrX <= mMaxX
                    currX = Math.min(currX, mMaxX)
                    currX = Math.max(currX, mMinX)
                    currY = startY + Math.round(distanceCoef * (mFinalY - startY))
                    // Pin to mMinY <= mCurrY <= mMaxY
                    currY = Math.min(currY, mMaxY)
                    currY = Math.max(currY, mMinY)
                    if (currX == mFinalX && currY == mFinalY) {
                        isFinished = true
                    }
                }
            }
        } else {
            currX = mFinalX
            currY = mFinalY
            isFinished = true
        }
        return true
    }
    /**
     * Start scrolling by providing a starting point, the distance to travel,
     * and the duration of the scroll.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     * numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     * will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     * content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     * content up.
     * @param duration Duration of the scroll in milliseconds.
     */
    /**
     * Start scrolling by providing a starting point and the distance to travel.
     * The scroll will use the default value of 250 milliseconds for the
     * duration.
     *
     * @param startX Starting horizontal scroll offset in pixels. Positive
     * numbers will scroll the content to the left.
     * @param startY Starting vertical scroll offset in pixels. Positive numbers
     * will scroll the content up.
     * @param dx Horizontal distance to travel. Positive numbers will scroll the
     * content to the left.
     * @param dy Vertical distance to travel. Positive numbers will scroll the
     * content up.
     */
    @JvmOverloads
    fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int = DEFAULT_DURATION) {
        mMode = SCROLL_MODE
        isFinished = false
        this.duration = duration
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        this.startX = startX
        this.startY = startY
        mFinalX = startX + dx
        mFinalY = startY + dy
        mDeltaX = dx.toFloat()
        mDeltaY = dy.toFloat()
        mDurationReciprocal = 1.0f / duration.toFloat()
    }

    /**
     * Start scrolling based on a fling gesture. The distance travelled will
     * depend on the initial velocity of the fling.
     *
     * @param startX Starting point of the scroll (X)
     * @param startY Starting point of the scroll (Y)
     * @param velocityX Initial velocity of the fling (X) measured in pixels per
     * second.
     * @param velocityY Initial velocity of the fling (Y) measured in pixels per
     * second
     * @param minX Minimum X value. The scroller will not scroll past this
     * point.
     * @param maxX Maximum X value. The scroller will not scroll past this
     * point.
     * @param minY Minimum Y value. The scroller will not scroll past this
     * point.
     * @param maxY Maximum Y value. The scroller will not scroll past this
     * point.
     */
    fun fling(
        startX: Int, startY: Int, velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int, minY: Int, maxY: Int
    ) {
        // Continue a scroll or fling in progress
        var velocityX = velocityX
        var velocityY = velocityY
        if (mFlywheel && !isFinished) {
            val oldVel = currVelocity
            val dx = (mFinalX - this.startX).toFloat()
            val dy = (mFinalY - this.startY).toFloat()
            val hyp = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
            val ndx = dx / hyp
            val ndy = dy / hyp
            val oldVelocityX = ndx * oldVel
            val oldVelocityY = ndy * oldVel
            if (Math.signum(velocityX.toFloat()) == Math.signum(oldVelocityX) &&
                Math.signum(velocityY.toFloat()) == Math.signum(oldVelocityY)
            ) {
                velocityX += oldVelocityX.toInt()
                velocityY += oldVelocityY.toInt()
            }
        }
        mMode = FLING_MODE
        isFinished = false
        val velocity = Math.hypot(velocityX.toDouble(), velocityY.toDouble()).toFloat()
        mVelocity = velocity
        duration = getSplineFlingDuration(velocity)
        mStartTime = AnimationUtils.currentAnimationTimeMillis()
        this.startX = startX
        this.startY = startY
        val coeffX = if (velocity == 0f) 1.0f else velocityX / velocity
        val coeffY = if (velocity == 0f) 1.0f else velocityY / velocity
        val totalDistance = getSplineFlingDistance(velocity)
        mDistance = (totalDistance * Math.signum(velocity)).toInt()
        mMinX = minX
        mMaxX = maxX
        mMinY = minY
        mMaxY = maxY
        mFinalX = startX + Math.round(totalDistance * coeffX).toInt()
        // Pin to mMinX <= mFinalX <= mMaxX
        mFinalX = Math.min(mFinalX, mMaxX)
        mFinalX = Math.max(mFinalX, mMinX)
        mFinalY = startY + Math.round(totalDistance * coeffY).toInt()
        // Pin to mMinY <= mFinalY <= mMaxY
        mFinalY = Math.min(mFinalY, mMaxY)
        mFinalY = Math.max(mFinalY, mMinY)
    }

    private fun getSplineDeceleration(velocity: Float): Double {
        return Math.log((INFLEXION * Math.abs(velocity) / (mFlingFriction * mPhysicalCoeff)).toDouble())
    }

    private fun getSplineFlingDuration(velocity: Float): Int {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne = DECELERATION_RATE - 1.0
        return (1000.0 * Math.exp(l / decelMinusOne)).toInt()
    }

    private fun getSplineFlingDistance(velocity: Float): Double {
        val l = getSplineDeceleration(velocity)
        val decelMinusOne = DECELERATION_RATE - 1.0
        return mFlingFriction * mPhysicalCoeff * Math.exp(DECELERATION_RATE / decelMinusOne * l)
    }

    /**
     * Stops the animation. Contrary to [.forceFinished],
     * aborting the animating cause the scroller to move to the final x and y
     * position
     *
     * @see .forceFinished
     */
    fun abortAnimation() {
        currX = mFinalX
        currY = mFinalY
        isFinished = true
    }

    /**
     * Extend the scroll animation. This allows a running animation to scroll
     * further and longer, when used with [.setFinalX] or [.setFinalY].
     *
     * @param extend Additional time to scroll in milliseconds.
     * @see .setFinalX
     * @see .setFinalY
     */
    fun extendDuration(extend: Int) {
        val passed = timePassed()
        duration = passed + extend
        mDurationReciprocal = 1.0f / duration
        isFinished = false
    }

    /**
     * Returns the time elapsed since the beginning of the scrolling.
     *
     * @return The elapsed time in milliseconds.
     */
    fun timePassed(): Int {
        return (AnimationUtils.currentAnimationTimeMillis() - mStartTime).toInt()
    }

    /**
     * @hide
     */
    fun isScrollingInDirection(xvel: Float, yvel: Float): Boolean {
        return !isFinished && sign(xvel) == sign((mFinalX - startX).toFloat()) && sign(yvel) == sign((mFinalY - startY).toFloat())
    }

    internal class ViscousFluidInterpolator : Interpolator {
        override fun getInterpolation(input: Float): Float {
            val interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input)
            return if (interpolated > 0) {
                interpolated + VISCOUS_FLUID_OFFSET
            } else interpolated
        }

        companion object {
            /** Controls the viscous fluid effect (how much of it).  */
            private const val VISCOUS_FLUID_SCALE = 8.0f
            private var VISCOUS_FLUID_NORMALIZE = 0f
            private var VISCOUS_FLUID_OFFSET = 0f

            init {

                // must be set to 1.0 (used in viscousFluid())
                VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f)
                // account for very small floating-point error
                VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f)
            }

            private fun viscousFluid(x: Float): Float {
                var x = x
                x *= VISCOUS_FLUID_SCALE
                if (x < 1.0f) {
                    x -= (1.0f - exp(-x.toDouble()).toFloat())
                } else {
                    val start = 0.36787944117f // 1/e == exp(-1)
                    x = 1.0f - exp((1.0f - x).toDouble()).toFloat()
                    x = start + x * (1.0f - start)
                }
                return x
            }
        }
    }

    companion object {
        private const val DEFAULT_DURATION = 250
        private const val SCROLL_MODE = 0
        private const val FLING_MODE = 1
        private val DECELERATION_RATE = (Math.log(0.78) / Math.log(0.9)).toFloat()
        private const val INFLEXION = 0.35f // Tension lines cross at (INFLEXION, 1)
        private const val START_TENSION = 0.5f
        private const val END_TENSION = 1.0f
        private const val P1 = START_TENSION * INFLEXION
        private const val P2 = 1.0f - END_TENSION * (1.0f - INFLEXION)
        private const val NB_SAMPLES = 100
        private val SPLINE_POSITION = FloatArray(NB_SAMPLES + 1)
        private val SPLINE_TIME = FloatArray(NB_SAMPLES + 1)

        init {
            var x_min = 0.0f
            var y_min = 0.0f
            for (i in 0 until NB_SAMPLES) {
                val alpha = i.toFloat() / NB_SAMPLES
                var x_max = 1.0f
                var x: Float
                var tx: Float
                var coef: Float
                while (true) {
                    x = x_min + (x_max - x_min) / 2.0f;
                coef = 3.0f * x * (1.0f - x);
                tx = coef * ((1.0f - x) * P1 + x * P2) + x * x * x;
                if (Math.abs(tx - alpha) < 1E-5) break;
                if (tx > alpha) x_max = x;
                else x_min = x;
                }
                SPLINE_POSITION[i] = coef * ((1.0f - x) * START_TENSION + x) + x * x * x;
                var y_max = 1.0f
                var y: Float
                var dy: Float
                while (true) {
                    y = y_min + (y_max - y_min) / 2.0f;
                    coef = 3.0f * y * (1.0f - y);
                    dy = coef * ((1.0f - y) * START_TENSION + y) + y * y * y;
                    if (Math.abs(dy - alpha) < 1E-5) break;
                    if (dy > alpha) y_max = y;
                    else y_min = y;
                }
                SPLINE_TIME[i] = coef * ((1.0f - y) * P1 + y * P2) + y * y * y;
            }
            SPLINE_TIME[NB_SAMPLES] = 1.0f
            SPLINE_POSITION[NB_SAMPLES] =
                SPLINE_TIME[NB_SAMPLES]
        }
    }
}