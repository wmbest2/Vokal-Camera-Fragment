package com.vokal.camera;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.View;

public class CameraActivity extends FragmentActivity implements CameraFragment.OnCameraEventListener {

    public static final String TAG = "CameraActivity";

    private CameraFragment mCamera;

    @Override
    public void onCreate(Bundle aSavedState) {
        super.onCreate(aSavedState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.image_capture);

        setupInterface();
        attachCameraFragment();
    }

    private void attachCameraFragment() {
        Fragment frag = getSupportFragmentManager().findFragmentByTag(CameraFragment.TAG);
        if (frag != null) {
            if (frag instanceof CameraFragment) {
                mCamera = (CameraFragment) frag;
                mCamera.setListener(this);
            }
        }
    }

    public void onPhotoCaptured() {
        displayReviewFragment();
    }

    private void displayReviewFragment() {
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.camera_interface, new ReviewPhotoFragment(), TAG);
        t.addToBackStack(TAG);
        t.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        t.commit();
    }

    public void onPhotoSaved(Uri aUri) {
        if (aUri != null) {
            Log.d("PHOTO", aUri.toString());
            finish();
        }
    }
    

    private void setupInterface() {
        if (getSupportFragmentManager() != null) {
            FragmentTransaction t = getSupportFragmentManager().beginTransaction();
            t.replace(R.id.camera_interface, new CameraOverlayFragment(), "overlay");
            t.commit();
        }
    }
}

