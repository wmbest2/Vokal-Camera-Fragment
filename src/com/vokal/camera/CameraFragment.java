package com.vokal.camera;

import android.content.*;
import android.content.res.Configuration;
import android.database.*;
import android.graphics.*;
import android.hardware.Camera;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.*;

import java.io.*;
import java.util.*;

public class CameraFragment extends Fragment {

    public static final String TAG = "CameraFragment";

    private static final int MIN_PREVIEW_PIXELS = 470 * 320; // small screen
    private static final int MAX_PREVIEW_PIXELS = 800 * 600; // large/HD screen

    private SurfaceView mSurface;
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private Point mScreenResolution;
    private Point mCameraResolution;

    private boolean mInPreview;
    private byte[] mLastPhoto;

    private OnCameraEventListener mListener;

    public OnCameraEventListener getListener() {
        return mListener;
    }
    
    public void setListener(final OnCameraEventListener aListener) {
        mListener = aListener;
    }
    

    @Override
    public View onCreateView(LayoutInflater aInflater, ViewGroup aContainer, Bundle aSavedState) {
        View result = getView();
        if (result == null) {
            result = aInflater.inflate(R.layout.camera, aContainer, false);

            mSurface = (SurfaceView) result.findViewById(R.id.surface);

            mHolder = mSurface.getHolder();
            mHolder.addCallback(mSurfaceCallback);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        return result;
    }

    @Override
    public void onCreate(Bundle aSavedState) {
        super.onCreate(aSavedState);
    }

    @Override
    public void onConfigurationChanged(Configuration aConfig) {
        super.onConfigurationChanged(aConfig);

        if (mInPreview) {
            //initFromCameraParameters(mCamera);
            // TODO Get rotation working properly
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mCamera=Camera.open();
        initFromCameraParameters(mCamera);
        mCamera.startPreview();
        mInPreview = true;
    }

    @Override
    public void onPause() {
        if (mInPreview) {
            mCamera.stopPreview();
        }
        mInPreview=false;
        mCamera.release();
        mCamera=null;

        super.onPause();
    }

    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        // We're landscape-only, and have apparently seen issues with display thinking it's portrait 
        // when waking from sleep. If it's not landscape, assume it's mistaken and reverse them:
        if (width < height) {
            Log.i(TAG, "Display reports portrait orientation; assuming this is incorrect");
            int temp = width;
            width = height;
            height = temp;
        }

        if (mHolder != null) {
            mHolder.setFixedSize(width, height);
        }

        mScreenResolution = new Point(width, height);
        Log.i(TAG, "Screen resolution: " + mScreenResolution);
        mCameraResolution = findBestPreviewSizeValue( new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes()), mScreenResolution);
        Log.i(TAG, "Camera resolution: " + mCameraResolution);

        parameters.setPreviewSize(mCameraResolution.x, mCameraResolution.y);               

        Point requested = new Point(1200, 800);
        Point min = new Point(640, 480);
        Point max = new Point(1600, 1200);

        Point mResolution = findClosestPhotoSize(requested, min, max);
        parameters.setPictureSize(mResolution.x, mResolution.y);               

        mCamera.setParameters(parameters);
    }

    SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback(){
        public void surfaceCreated(SurfaceHolder aHolder) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void surfaceChanged(SurfaceHolder aHolder,
                int aFormat, 
                int aWidth,
                int aHeight) {
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };

    private Comparator mSizeCompare = new Comparator<Camera.Size>() {
        @Override
        public int compare(Camera.Size a, Camera.Size b) {
            int aPixels = a.height * a.width;
            int bPixels = b.height * b.width;
            if (bPixels < aPixels) {
                return -1;
            }
            if (bPixels > aPixels) {
                return 1;
            }
            return 0;
        }
    };
    
    private Point findClosestPhotoSize(Point aRequested, Point aMin, Point aMax) {
        ArrayList<Camera.Size> list = new ArrayList<Camera.Size>(mCamera.getParameters().getSupportedPictureSizes());
        Collections.sort(list, mSizeCompare);

        Point bestSize = null;

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size supportedPreviewSize : list) {
                previewSizesString.append(supportedPreviewSize.width).append('x')
                    .append(supportedPreviewSize.height).append(' ');
            }
            Log.i(TAG, "Supported preview sizes: " + previewSizesString);
        }


        float screenAspectRatio = (float) mScreenResolution.x / (float) mScreenResolution.y;

        float diff = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedSize : list) {
            int realWidth = supportedSize.width;
            int realHeight = supportedSize.height;

            int minPixels = aMin.y * aMin.x;
            int maxPixels = aMax.y * aMax.x;

            int pixels = realWidth * realHeight;
            
            if (pixels < minPixels || pixels > maxPixels) {
                continue;
            }
            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == aRequested.x && maybeFlippedHeight == aRequested.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = new Point(realWidth, realHeight);
                diff = newDiff;
            }
        }

        if (bestSize == null) {
            Camera.Size defaultSize = mCamera.getParameters().getPreviewSize();
            bestSize = new Point(defaultSize.width, defaultSize.height);
            Log.i(TAG, "No suitable preview sizes, using default: " + bestSize);
        }

        Log.i(TAG, "Found best approximate image size: " + bestSize);
        return bestSize;
    }

    private Point findBestPreviewSizeValue(List<Camera.Size> supportedPreviewSizes, Point mScreenResolution) {

        Collections.sort(supportedPreviewSizes, mSizeCompare);

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                previewSizesString.append(supportedPreviewSize.width).append('x')
                    .append(supportedPreviewSize.height).append(' ');
            }
            Log.i(TAG, "Supported preview sizes: " + previewSizesString);
        }

        Point bestSize = null;
        float screenAspectRatio = (float) mScreenResolution.x / (float) mScreenResolution.y;

        float diff = Float.POSITIVE_INFINITY;
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            int pixels = realWidth * realHeight;
            if (pixels < MIN_PREVIEW_PIXELS || pixels > MAX_PREVIEW_PIXELS) {
                continue;
            }
            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;
            if (maybeFlippedWidth == mScreenResolution.x && maybeFlippedHeight == mScreenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.i(TAG, "Found preview size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }
            float aspectRatio = (float) maybeFlippedWidth / (float) maybeFlippedHeight;
            float newDiff = Math.abs(aspectRatio - screenAspectRatio);
            if (newDiff < diff) {
                bestSize = new Point(realWidth, realHeight);
                diff = newDiff;
            }
        }

        //if (bestSize == null) {
            //Camera.Size defaultSize = parameters.getPreviewSize();
            //bestSize = new Point(defaultSize.width, defaultSize.height);
            //Log.i(TAG, "No suitable preview sizes, using default: " + bestSize);
        //}

        Log.i(TAG, "Found best approximate preview size: " + bestSize);
        return bestSize;
    }

    private static String findSettableValue(Collection<String> supportedValues,
            String... desiredValues) {
        Log.i(TAG, "Supported values: " + supportedValues);
        String result = null;
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        Log.i(TAG, "Settable value: " + result);
        return result;
    }

    public void takePhoto() {
        if (mCamera != null) {
            mInPreview = false;
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean aSuccess, Camera aCamera) {
                    if (aSuccess) {
                        take();
                    }
                }
            });
        }
    }

    private void take() {
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            public void onPictureTaken(byte[] aData, Camera aCamera) {
                Log.d(TAG, "Got Photo " + aData.toString());
                mLastPhoto = aData;

                if (mListener != null) {
                    mListener.onPhotoCaptured();
                }
            }
        });
    }

    private class SaveTask extends AsyncTask<Void, Void, Uri> {
        
        @Override
        public Uri doInBackground(Void... aParams) {
            Log.d(TAG, "Has Photo: " + mLastPhoto.toString());
            if (mLastPhoto != null) {
                Log.d(TAG, "Has Photo");
                ContentValues values = new ContentValues();
                values.put(Images.Media.TITLE, String.format("claim_image_%d", System.currentTimeMillis()));
                values.put(Images.Media.BUCKET_ID, "snapsheet");
                values.put(Images.Media.MIME_TYPE, "image/jpeg");
                Log.d(TAG, "Has values " + values.toString());
                Uri uri = null;
                try {
                    uri = getActivity().getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
                    Log.d(TAG, "Has uri " + uri.toString());
                    Bitmap x = BitmapFactory.decodeByteArray(mLastPhoto, 0, mLastPhoto.length);
                    OutputStream outstream;
                    outstream = getActivity().getContentResolver().openOutputStream(uri);
                    x.compress(Bitmap.CompressFormat.JPEG, 80, outstream);
                    outstream.close();

                    long id = ContentUris.parseId(uri);
                    Images.Thumbnails.getThumbnail(getActivity().getContentResolver(),
                        id, Images.Thumbnails.MICRO_KIND, null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }catch (IOException e){
                    e.printStackTrace();
                    return null;
                }

                return uri;
            }
            return null;
        }

        public void onPostExecute(Uri aResult) {
            if (mListener != null) {
                mListener.onPhotoSaved(aResult);
            }
        }
    }

    public void savePhoto() {
        new SaveTask().execute();
    }

    public void restartCamera() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public boolean isFlashAvailable() {
        return true;
    }

    public boolean toggleFlash() {
        return false;
    }

    public interface OnCameraEventListener {
        public abstract void onPhotoCaptured(); 
        public abstract void onPhotoSaved(Uri aUri);
    }
}
