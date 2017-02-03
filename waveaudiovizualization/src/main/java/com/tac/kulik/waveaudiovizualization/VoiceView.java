package com.tac.kulik.waveaudiovizualization;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.tac.kulik.waveaudiovizualization.util.ScreenUtils;

/**
 * Created by kulik on 01.02.17.
 */
public class VoiceView extends View {

    private static final String TAG = VoiceView.class.getName();

    private Paint mPaint;
    private AnimatorSet mAnimatorSet = new AnimatorSet();

    private float mMinRadius;
    private float mMaxRadius;
    private float mCurrentRadius;

    private int mColor;
    private float mScale;

    public VoiceView(Context context) {
        super(context);
        init();
    }

    public VoiceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.VoiceView);
        mColor = attr.getColor(R.styleable.VoiceView_color, Color.argb(128, 219, 219, 219));
        mScale = attr.getFloat(R.styleable.VoiceView_scale, 1f);
        attr.recycle();
        init();
    }

    private void init() {

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mColor);

        mMinRadius = ScreenUtils.dp2px(getContext(), 50) / 2;
        mCurrentRadius = mMinRadius;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mMaxRadius = Math.min(w, h) / 2;
        Log.d(TAG, "MaxRadius: " + mMaxRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        if (mCurrentRadius > mMinRadius) {
            canvas.drawCircle(width / 2, height / 2, mCurrentRadius * mScale, mPaint);
        }
    }

    public void animateRadius(float radius) {
        if (radius <= mCurrentRadius) {
            return;
        }
        if (radius > mMaxRadius) {
            radius = mMaxRadius;
        } else if (radius < mMinRadius) {
            radius = mMinRadius;
        }
        if (radius == mCurrentRadius) {
            return;
        }
        if (mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel();
        }
        mAnimatorSet.playSequentially(
                ObjectAnimator.ofFloat(this, "CurrentRadius", getCurrentRadius(), radius).setDuration(50),
                ObjectAnimator.ofFloat(this, "CurrentRadius", radius, mMinRadius).setDuration(600)
        );
        mAnimatorSet.start();
    }

    public float getCurrentRadius() {
        return mCurrentRadius;
    }

    public void setCurrentRadius(float currentRadius) {
        mCurrentRadius = currentRadius;
        invalidate();
    }

}