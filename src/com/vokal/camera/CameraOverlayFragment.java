package com.vokal.camera;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.*;

public class CameraOverlayFragment extends Fragment {

    public static final String TAG = "CameraOverlayFragment";

    private CameraFragment mCamera;

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = getView();
        if (result == null) {
            result = aInflater.inflate(R.layout.camera_overlay, aContainer, false);

            result.findViewById(R.id.shutter).setOnClickListener(new View.OnClickListener() {
                public void onClick(View aView) {
                    if (mCamera != null) {
                        mCamera.takePhoto();
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

    private void attachCameraFragment() {
        Fragment frag = getFragmentManager().findFragmentByTag(CameraFragment.TAG);
        if (frag != null) {
            if (frag instanceof CameraFragment) {
                mCamera = (CameraFragment) frag;
            }
        }
    }

}
