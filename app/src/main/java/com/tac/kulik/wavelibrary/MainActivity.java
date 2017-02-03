package com.tac.kulik.wavelibrary;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tac.kulik.waveaudiovizualization.VoiceWaveView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST = 1;
    private MediaRecorder mMediaRecorder;
    private Handler mHandler;
    private VoiceWaveView mWaveView;
    private boolean mIsRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWaveView = (VoiceWaveView) findViewById(R.id.waveView);
        mHandler = new Handler(Looper.getMainLooper());
        checkPerm();

    }

    private void checkPerm() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            startVizualization();
        }

    }

    private void startVizualization() {
        try {
            if (mMediaRecorder != null) {
                mMediaRecorder.release();
            }
            File tmp = new File(getCacheDir(), "tmp.amr");
            tmp.createNewFile();
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setOutputFile(tmp.getAbsolutePath());
            //set 60 seconds max duration
            mMediaRecorder.setMaxDuration(60 * 1000);
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mIsRecording = true;

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWaveView.update(mMediaRecorder.getMaxAmplitude());
                            mHandler.postDelayed(this, 50);
                        }
                    });
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, "MediaRecorder prepare failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    startVizualization();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        if (mMediaRecorder != null) {
            if (mIsRecording) {
                mMediaRecorder.stop();
            }
            mMediaRecorder.release();
            mIsRecording = false;
        }
    }
}
