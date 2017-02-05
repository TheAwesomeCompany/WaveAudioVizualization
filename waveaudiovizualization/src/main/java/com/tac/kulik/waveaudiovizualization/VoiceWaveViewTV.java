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
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;

import com.tac.kulik.waveaudiovizualization.util.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kulik on 01.02.17.
 */
public class VoiceWaveViewTV extends TextureView {

    private static final String TAG = VoiceWaveViewTV.class.getName();

    private static final int K = 8;
    private int mNumberOfWaves = 4;

    private static final float[] KOF = new float[]{1f, -0.66f, 0.33f, -0.25f};
    private AnimatorSet mAnimatorSet = new AnimatorSet();
    private int mMaxAmpl;
    private volatile float mCurrentAmpl;
    private int mWidth;
    private int mHeight;
    private List<Paint> mPaints;
    private Renderer mRenderer;

    public VoiceWaveViewTV(Context context) {
        super(context);
        init();
    }

    public VoiceWaveViewTV(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mPaints = new ArrayList<Paint>();
        Paint paint;
        for (int i = 0; i < mNumberOfWaves; i++) {
            paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
//        mPaint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.BUTT);
            paint.setStrokeJoin(Paint.Join.ROUND);
            mPaints.add(paint);

        }
        mRenderer = new Renderer();
        mRenderer.start();
        setSurfaceTextureListener(mRenderer);
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

    int t = 0;


    private class Renderer extends Thread implements TextureView.SurfaceTextureListener {
        private Object mLock = new Object();        // guards mSurfaceTexture, mDone
        private SurfaceTexture mSurfaceTexture;
        private boolean mDone;

        public Renderer() {
            super("TextureViewCanvas Renderer");
        }

        @Override
        public void run() {
            while (true) {
                SurfaceTexture surfaceTexture = null;

                // Latch the SurfaceTexture when it becomes available.  We have to wait for
                // the TextureView to create it.
                synchronized (mLock) {
                    while (!mDone && (surfaceTexture = mSurfaceTexture) == null) {
                        try {
                            mLock.wait();
                        } catch (InterruptedException ie) {
                            throw new RuntimeException(ie);     // not expected
                        }
                    }
                    if (mDone) {
                        break;
                    }
                }
                Log.d(TAG, "Got surfaceTexture=" + surfaceTexture);

                // Render frames until we're told to stop or the SurfaceTexture is destroyed.
                doAnimation();
            }

            Log.d(TAG, "Renderer thread exiting");
        }

        /**
         * Draws updates as fast as the system will allow.
         * <p>
         * In 4.4, with the synchronous buffer queue queue, the frame rate will be limited.
         * In previous (and future) releases, with the async queue, many of the frames we
         * render may be dropped.
         * <p>
         * The correct thing to do here is use Choreographer to schedule frame updates off
         * of vsync, but that's not nearly as much fun.
         */
        private void doAnimation() {

            // Create a Surface for the SurfaceTexture.
            Surface surface = null;
            synchronized (mLock) {
                SurfaceTexture surfaceTexture = mSurfaceTexture;
                if (surfaceTexture == null) {
                    Log.d(TAG, "ST null on entry");
                    return;
                }
                surface = new Surface(surfaceTexture);
            }

            while (true) {
                Canvas canvas = surface.lockCanvas(null);
                if (canvas == null) {
                    Log.d(TAG, "lockCanvas() failed");
                    break;
                }
                try {
                    canvas.drawColor(Color.WHITE);
                    for (int i = 0; i < mNumberOfWaves; i++) {
                        float halfHeight = mHeight / 2.0f;
                        float scaling = (float) KOF[i];
                        Path pth = new Path();
                        pth.moveTo(0, halfHeight);
                        float ampl = mCurrentAmpl;
                        float phi = i * (2 * mCurrentAmpl / mMaxAmpl) / 10 + t / 5f;
//                        float phi = i * ampl / 10f + t / 10f;
                        for (int x = 0; x < mWidth; x++) {
                            // We use a parable to scale the sinus wave, that has its peak in the middle of the view.
                            float v = (1.5f * (((float) x) / mWidth * 2) - 1.5f);
                            float y = (float) (scaling * ampl * Math.pow(K / (K + Math.pow(v, 4)), K) * Math.cos(4 * v - phi) + halfHeight + scaling * ampl * 0.05f * Math.cos(3 * v));
                            pth.lineTo(x, y);
                        }
                        canvas.drawPath(pth, mPaints.get(i));
                    }
                    t++;
                } finally {
                    // Publish the frame.  If we overrun the consumer, frames will be dropped,
                    // so on a sufficiently fast device the animation will run at faster than
                    // the display refresh rate.
                    //
                    // If the SurfaceTexture has been destroyed, this will throw an exception.
                    try {
                        surface.unlockCanvasAndPost(canvas);
                    } catch (IllegalArgumentException iae) {
                        Log.d(TAG, "unlockCanvasAndPost failed: " + iae.getMessage());
                        break;
                    }
                }
            }

            surface.release();
        }

        /**
         * Tells the thread to stop running.
         */

        public void halt() {
            synchronized (mLock) {
                mDone = true;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureAvailable(SurfaceTexture st, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable(" + width + "x" + height + ")");
            mWidth = width;
            mHeight = height;
            mMaxAmpl = height / 2;
            mPaints.get(0).setShader(new LinearGradient(0, 0, mWidth, mHeight, Color.parseColor("#CA67B7"), Color.parseColor("#C081C1"), Shader.TileMode.MIRROR));
            mPaints.get(0).setStrokeWidth(ScreenUtils.dp2px(getContext(), 2));
            mPaints.get(1).setColor(Color.parseColor("#D5E3FA"));
            mPaints.get(1).setStrokeWidth(ScreenUtils.dp2px(getContext(), 2));
            mPaints.get(2).setStrokeWidth(ScreenUtils.dp2px(getContext(), 1));
            mPaints.get(3).setStrokeWidth(ScreenUtils.dp2px(getContext(), 1));
            mPaints.get(2).setShader(new LinearGradient(0, 0, mWidth, mHeight, Color.parseColor("#E67EA5"), Color.parseColor("#7F7AE1"), Shader.TileMode.MIRROR));
            mPaints.get(3).setShader(new LinearGradient(0, 0, mWidth, mHeight, Color.parseColor("#E67EA5"), Color.parseColor("#7F7AE1"), Shader.TileMode.MIRROR));
            synchronized (mLock) {
                mSurfaceTexture = st;
                mLock.notify();
            }
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureSizeChanged(SurfaceTexture st, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged(" + width + "x" + height + ")");
            mWidth = width;
            mHeight = height;
            mMaxAmpl = height / 2;
//        mCurrentAmpl = mMaxAmpl;
        }

        @Override   // will be called on UI thread
        public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
            Log.d(TAG, "onSurfaceTextureDestroyed");

            synchronized (mLock) {
                mSurfaceTexture = null;
            }
            return true;
        }

        @Override   // will be called on UI thread
        public void onSurfaceTextureUpdated(SurfaceTexture st) {
            //Log.d(TAG, "onSurfaceTextureUpdated");
        }
    }
}


//
//    class Drawer implements Runnable {
//        @Override
//        public void run() {
//            while (mIsDrawing) {
//                Canvas canvas = getHolder().lockCanvas();
//                if (canvas != null) {
//                    canvas.drawColor(Color.WHITE);
//                    for (int i = 0; i < mNumberOfWaves; i++) {
//                        float halfHeight = mHeight / 2.0f;
//                        float scaling = (float) KOF[i];
//                        Path pth = new Path();
//                        pth.moveTo(0, halfHeight);
//                        float ampl = mCurrentAmpl;
//                        float phi = i * (2 * mCurrentAmpl/mMaxAmpl) / 10 + t / 5f;
////                        float phi = i * ampl / 10f + t / 10f;
//                        for (int x = 0; x < mWidth; x++) {
//                            // We use a parable to scale the sinus wave, that has its peak in the middle of the view.
//                            float v = (1.5f * (((float) x) / mWidth * 2) - 1.5f);
//                            float y = (float) (scaling * ampl * Math.pow(K / (K + Math.pow(v, 4)), K) * Math.cos(4 * v - phi) + halfHeight + scaling*ampl*0.05f* Math.cos(3 * v));
//                            pth.lineTo(x, y);
//                        }
//                        canvas.drawPath(pth, mPaints.get(i));
//                    }
//                    getHolder().unlockCanvasAndPost(canvas);
//                    t++;
////                    try {
////                        Thread.sleep(50);
////                    } catch (InterruptedException e) {
////                        e.printStackTrace();
////                    }
//                }
//            }
//
//        }
//    }