package com.jingran.taskmanager.ui.animation

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.google.android.material.shape.MaterialShapeDrawable

object RippleEffectUtils {

    fun applyRippleEffect(view: View, radius: Float = ViewConfiguration.getTapTimeout().toFloat()) {
        ViewCompat.setBackground(view, createRippleDrawable(view.context, radius))
    }

    fun createRippleDrawable(context: Context, radius: Float): Drawable {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            RippleDrawable(
                getRippleColor(context),
                null,
                getRippleMask(context, radius)
            )
        } else {
            createLegacyRippleDrawable(context)
        }
    }

    private fun getRippleColor(context: Context): ColorStateList {
        return ResourcesCompat.getColorStateList(
            context.resources,
            com.google.android.material.R.attr.colorControlHighlight,
            context.theme
        ) ?: ColorStateList.valueOf(Color.argb(50, 0, 0, 0))
    }

    private fun getRippleMask(context: Context, radius: Float): Drawable {
        val shapeDrawable = MaterialShapeDrawable()
        shapeDrawable.setCornerSize(radius)
        shapeDrawable.setFillColor(Color.WHITE)
        return shapeDrawable
    }

    private fun createLegacyRippleDrawable(context: Context): Drawable {
        val paint = Paint().apply {
            color = Color.argb(50, 0, 0, 0)
            style = Paint.Style.FILL
        }
        
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                canvas.drawPaint(paint)
            }
            
            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }
            
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }
            
            override fun getOpacity(): Int {
                return android.graphics.PixelFormat.TRANSLUCENT
            }
            
            override fun getIntrinsicWidth(): Int {
                return -1
            }
            
            override fun getIntrinsicHeight(): Int {
                return -1
            }
        }
    }

    fun createCircularRipple(context: Context, radius: Float): RippleDrawable {
        return RippleDrawable(
            getRippleColor(context),
            createCircularMask(radius)
        )
    }

    private fun createCircularMask(radius: Float): Drawable {
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                val centerX = bounds.exactCenterX()
                val centerY = bounds.exactCenterY()
                canvas.drawCircle(centerX, centerY, radius, paint)
            }
            
            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }
            
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }
            
            override fun getOpacity(): Int {
                return android.graphics.PixelFormat.TRANSLUCENT
            }
            
            override fun getIntrinsicWidth(): Int {
                return (radius * 2).toInt()
            }
            
            override fun getIntrinsicHeight(): Int {
                return (radius * 2).toInt()
            }
        }
    }

    fun createRectangularRipple(context: Context, cornerRadius: Float): RippleDrawable {
        return RippleDrawable(
            getRippleColor(context),
            createRectangularMask(cornerRadius)
        )
    }

    private fun createRectangularMask(cornerRadius: Float): Drawable {
        val paint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        return object : Drawable() {
            override fun draw(canvas: Canvas) {
                val rectF = RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
                canvas.drawRoundRect(
                    rectF,
                    cornerRadius,
                    cornerRadius,
                    paint
                )
            }
            
            override fun setAlpha(alpha: Int) {
                paint.alpha = alpha
            }
            
            override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                paint.colorFilter = colorFilter
            }
            
            override fun getOpacity(): Int {
                return android.graphics.PixelFormat.TRANSLUCENT
            }
            
            override fun getIntrinsicWidth(): Int {
                return bounds.width()
            }
            
            override fun getIntrinsicHeight(): Int {
                return bounds.height()
            }
        }
    }

    fun performHapticFeedback(view: View) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }

    fun performHapticFeedback(view: View, feedbackConstant: Int) {
        view.performHapticFeedback(feedbackConstant)
    }

    fun animateRipple(view: View, x: Float, y: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val rippleDrawable = view.background as? RippleDrawable
            rippleDrawable?.setHotspot(x, y)
            rippleDrawable?.setVisible(true, true)
        }
    }

    fun hideRipple(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val rippleDrawable = view.background as? RippleDrawable
            rippleDrawable?.setVisible(false, true)
        }
    }
}