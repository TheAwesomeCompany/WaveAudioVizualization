package com.tac.kulik.waveaudiovizualization;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.tac.kulik.waveaudiovizualization.util.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kulik on 01.02.17.
 */
public class VoiceWaveView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = VoiceWaveView.class.getName();

    private static final int K = 8;
    private int mNumberOfWaves = 4;

    private static final float[] KOF = new float[]{1f, -0.66f, 0.33f, -0.25f};
    private AnimatorSet mAnimatorSet = new AnimatorSet();
    private int mMaxAmpl;
    private boolean mIsDrawing;
    private volatile float mCurrentAmpl;
    private int mWidth;
    private int mHeight;
    private List<Paint> mPaints;

    public VoiceWaveView(Context context) {
        super(context);
        init();
    }

    public VoiceWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        mPaints = new ArrayList<Paint>();
            Paint paint;
        for (int i = 0; i< mNumberOfWaves; i++) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
//        mPaint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeJoin(Paint.Join.ROUND);
            mPaints.add(paint);

        }
//        mPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    }

    public float getCurrentAmpl() {
        return mCurrentAmpl;
    }

    public void update(float set) {
        set = (float) Math.log10(Math.max(1, set - 500)) * mMaxAmpl / 4f;
//        if (set == mCurrentAmpl) {
//            return;
//        }
        if (set <= mCurrentAmpl) {
            return;
        }
        if (mAnimatorSet.isRunning()) {
            mAnimatorSet.cancel();
        }
        mAnimatorSet.playSequentially(
                ObjectAnimator.ofFloat(this, "CurrentAmpl", mCurrentAmpl, set).setDuration(50),
                ObjectAnimator.ofFloat(this, "CurrentAmpl", set, 0).setDuration(600)
        );
        mAnimatorSet.start();

    }

    public void setCurrentAmpl(float currentAmpl) {
        mCurrentAmpl = currentAmpl;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mIsDrawing = true;
        new Thread(new Drawer()).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        mHeight = h;
        mMaxAmpl = h / 2;
//        mCurrentAmpl = mMaxAmpl;
        mWidth = w;
        mPaints.get(0).setShader(new LinearGradient(0, 0, mWidth, mHeight, Color.parseColor("#CA67B7"), Color.parseColor("#C081C1"), Shader.TileMode.MIRROR));
        mPaints.get(0).setStrokeWidth(ScreenUtils.dp2px(getContext(), 2));
        mPaints.get(1).setColor(Color.parseColor("#D5E3FA"));
        mPaints.get(1).setStrokeWidth(ScreenUtils.dp2px(getContext(), 2));
        mPaints.get(2).setStrokeWidth(ScreenUtils.dp2px(getContext(), 1));
        mPaints.get(3).setStrokeWidth(ScreenUtils.dp2px(getContext(), 1));
        mPaints.get(2).setShader(new LinearGradient(0, 0, mWidth, mHeight, Color.parseColor("#E67EA5"), Color.parseColor("#7F7AE1"), Shader.TileMode.MIRROR));
        mPaints.get(3).setShader(new LinearGradient(0, 0, mWidth, mHeight, Color.parseColor("#E67EA5"), Color.parseColor("#7F7AE1"), Shader.TileMode.MIRROR));
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mIsDrawing = false;
    }

    int t = 0;

    class Drawer implements Runnable {
        @Override
        public void run() {
            while (mIsDrawing) {
                Canvas canvas = getHolder().lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(Color.WHITE);
                    for (int i = 0; i < mNumberOfWaves; i++) {
                        float halfHeight = mHeight / 2.0f;
                        float scaling = (float) KOF[i];
                        Path pth = new Path();
                        pth.moveTo(0, halfHeight);
                        float ampl = mCurrentAmpl;
                        float phi = i * (2 * mCurrentAmpl/mMaxAmpl) / 10 + t / 5f;
//                        float phi = i * ampl / 10f + t / 10f;
                        for (int x = 0; x < mWidth; x++) {
                            // We use a parable to scale the sinus wave, that has its peak in the middle of the view.
                            float v = (1.5f * (((float) x) / mWidth * 2) - 1.5f);
                            float y = (float) (scaling * ampl * Math.pow(K / (K + Math.pow(v, 4)), K) * Math.cos(4 * v - phi) + halfHeight + scaling*ampl*0.05f* Math.cos(3 * v));
                            pth.lineTo(x, y);
                        }
                        canvas.drawPath(pth, mPaints.get(i));
                    }
                    getHolder().unlockCanvasAndPost(canvas);
                    t++;
//                    try {
//                        Thread.sleep(50);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
            }

        }
    }
}