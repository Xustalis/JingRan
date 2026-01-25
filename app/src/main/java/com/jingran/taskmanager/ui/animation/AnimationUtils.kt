package com.jingran.taskmanager.ui.animation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialSharedAxis

object AnimationUtils {

    private const val DEFAULT_DURATION = 300L
    private const val SHORT_DURATION = 150L
    private const val LONG_DURATION = 500L

    fun fadeIn(view: View, duration: Long = DEFAULT_DURATION) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun fadeOut(view: View, duration: Long = DEFAULT_DURATION, onEnd: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    fun slideInFromRight(view: View, duration: Long = DEFAULT_DURATION) {
        view.translationX = view.width.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, view.width.toFloat(), 0f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            )
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun slideInFromBottom(view: View, duration: Long = DEFAULT_DURATION) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.height.toFloat(), 0f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            )
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun slideOutToLeft(view: View, duration: Long = DEFAULT_DURATION, onEnd: (() -> Unit)? = null) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -view.width.toFloat()),
                ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
            )
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.translationX = 0f
                    view.alpha = 1f
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    fun scaleIn(view: View, duration: Long = DEFAULT_DURATION) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.alpha = 0f
        view.visibility = View.VISIBLE
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
            )
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun scaleOut(view: View, duration: Long = DEFAULT_DURATION, onEnd: (() -> Unit)? = null) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 0f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 0f),
                ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f)
            )
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    view.scaleX = 1f
                    view.scaleY = 1f
                    view.alpha = 1f
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    fun rotate(view: View, fromDegrees: Float, toDegrees: Float, duration: Long = DEFAULT_DURATION) {
        ObjectAnimator.ofFloat(view, View.ROTATION, fromDegrees, toDegrees).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun pulse(view: View, duration: Long = 1000L, repeatCount: Int = 1) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration / 2
            repeatCount = repeatCount * 2
            repeatMode = ValueAnimator.REVERSE
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                val scale = 1f + (animatedValue * 0.1f)
                view.scaleX = scale
                view.scaleY = scale
            }
            start()
        }
    }

    fun shake(view: View, duration: Long = 500L) {
        val shakeDistance = 10f
        ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                val translation = (Math.sin(animatedValue * Math.PI * 4) * shakeDistance).toFloat()
                view.translationX = translation
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.translationX = 0f
                }
            })
            start()
        }
    }

    fun expand(view: View, targetHeight: Int, duration: Long = DEFAULT_DURATION) {
        view.measure(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val startHeight = view.measuredHeight
        
        ValueAnimator.ofInt(startHeight, targetHeight).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                view.layoutParams.height = animatedValue
                view.requestLayout()
            }
            start()
        }
    }

    fun collapse(view: View, duration: Long = DEFAULT_DURATION, onEnd: (() -> Unit)? = null) {
        ValueAnimator.ofInt(view.height, 0).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Int
                view.layoutParams.height = animatedValue
                view.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    fun animateRecyclerViewItemAdded(viewHolder: RecyclerView.ViewHolder) {
        fadeIn(viewHolder.itemView, SHORT_DURATION)
    }

    fun animateRecyclerViewItemRemoved(viewHolder: RecyclerView.ViewHolder, onEnd: (() -> Unit)? = null) {
        fadeOut(viewHolder.itemView, SHORT_DURATION) {
            onEnd?.invoke()
        }
    }

    fun animateRecyclerViewItemChanged(oldViewHolder: RecyclerView.ViewHolder?, newViewHolder: RecyclerView.ViewHolder) {
        oldViewHolder?.let {
            fadeOut(it.itemView, SHORT_DURATION / 2)
        }
        fadeIn(newViewHolder.itemView, SHORT_DURATION / 2)
    }

    fun animateFabExpand(fab: FloatingActionButton) {
        fab.setImageResource(android.R.drawable.ic_input_add)
        fab.size = FloatingActionButton.SIZE_NORMAL
        scaleIn(fab, SHORT_DURATION)
    }

    fun animateFabCollapse(fab: FloatingActionButton) {
        fab.setImageResource(android.R.drawable.ic_menu_add)
        fab.size = FloatingActionButton.SIZE_MINI
        scaleOut(fab, SHORT_DURATION)
    }

    fun createMaterialFade(): MaterialFade {
        return MaterialFade().apply {
            duration = DEFAULT_DURATION
            interpolator = DecelerateInterpolator()
        }
    }

    fun createMaterialSharedAxis(forward: Boolean = true): MaterialSharedAxis {
        return MaterialSharedAxis(MaterialSharedAxis.Y, forward).apply {
            duration = DEFAULT_DURATION
            interpolator = DecelerateInterpolator()
        }
    }

    fun createMaterialContainerTransform(): MaterialContainerTransform {
        return MaterialContainerTransform().apply {
            duration = DEFAULT_DURATION
            interpolator = DecelerateInterpolator()
        }
    }

    fun animateViewGroupChildren(viewGroup: ViewGroup, duration: Long = DEFAULT_DURATION, staggerDelay: Long = 50L) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            child.alpha = 0f
            child.visibility = View.VISIBLE
            
            ObjectAnimator.ofFloat(child, View.ALPHA, 0f, 1f).apply {
                this.duration = duration
                this.startDelay = i * staggerDelay
                interpolator = DecelerateInterpolator()
                start()
            }
        }
    }

    fun rippleEffect(view: View, x: Float, y: Float) {
        view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun successAnimation(view: View) {
        pulse(view, 500L, 2)
    }

    fun errorAnimation(view: View) {
        shake(view, 500L)
    }
}