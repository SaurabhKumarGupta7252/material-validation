package com.android.app.speedviewlib.components.indicators

import android.content.Context
import android.graphics.Canvas

/**
 * this Library build By Saurabh Gupta
 */
class NoIndicator(context: Context) : Indicator<NoIndicator>(context) {

    override fun draw(canvas: Canvas) {}

    override fun updateIndicator() {}

    override fun setWithEffects(withEffects: Boolean) {}
}
