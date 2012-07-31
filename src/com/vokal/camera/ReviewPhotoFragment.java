package com.vokal.camera;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;

public class ReviewPhotoFragment extends Fragment {

    private CameraFragment mCamera;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = getView();
        if (result == null) {
            result = aInflater.inflate(R.layout.camera_review, aContainer, false);

            result.findViewById(R.id.flash).setOnClickListener(new View.OnClickListener() {
                public void onClick(View aView) {
                    if (mCamera != null) {
                        mCamera.restartCamera();
                        getFragmentManager().popBackStack();
                    }
                }
            });

            result.findViewById(R.id.shutter).setOnClickListener(new View.OnClickListener() {
                public void onClick(View aView) {
                    if (mCamera != null) {
                        mCamera.savePhoto();
                    }
                }
            });
        }
        
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();

        attachCameraFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCamera.restartCamera();
    }

    private void attachCameraFragment() {
        Fragment frag = getFragmentManager().findFragmentByTag(CameraFragment.TAG);
        if (frag != null) {
            if (frag instanceof CameraFragment) {
                mCamera = (CameraFragment) frag;
            }
        }
    }
}
