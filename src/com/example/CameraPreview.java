package com.example;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/*
 * SurfaceView is the object in charge of handling the calls and painting
 * the images processed through the GPU
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    //Load the native library compiled for this project
	static {
        System.loadLibrary("JNIProcessor");
    }

	// Tag used in the LogCat
	private static final String TAG = "CameraPreview";

    private Camera 	mCamera;
    private byte[] 	mVideoSource;
    private Bitmap 	mBackBuffer;
    private int[]	mImgDims;
    private Paint 	mPaint;
    private Rect	mSrcRect;
    private Rect	mTrgtRect;
    private int     mSelFilter;
    private String  mDisplayStr;

    //Native method which applies Laplacian to the image
    native private void runfilter(Bitmap out, byte[] in, int width, int height, int choice);

    public CameraPreview(Context context) {
        super(context);
        
        getHolder().addCallback(this);	//this is the receiver of all callbacks
        setWillNotDraw(false);			//let's take care of the drawing
        mImgDims 	= new int[3];
        mImgDims[2] = 4;
        //Text painter
        mPaint 		= new Paint();
        mPaint.setTextSize(64);
        mPaint.setColor(0xFFFF0000);
        mDisplayStr = new String("RGBA");
    }

    /*
     * Camera is created and initialized
     */
    public void surfaceCreated(SurfaceHolder pHolder) {
        try {
            mCamera = Camera.open(0);
            Camera.Parameters params = mCamera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            params.setPreviewSize(Constants.MAX_DISP_IMG_WIDTH,
                    Constants.MAX_DISP_IMG_HEIGHT);
            params.setPreviewFpsRange(Constants.MIN_FPS,Constants.MAX_FPS);
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(0);
            mCamera.setPreviewDisplay(null);
            mCamera.setPreviewCallbackWithBuffer(this);
        } catch (IOException eIOException) {
            Log.i(TAG, "Error setting camera preview: " + eIOException.getMessage());
            throw new IllegalStateException();
        }
        catch (Exception e) {
            Log.i(TAG, e.getMessage());
        }
    }

    /*
     * Stop preview events, reset the camera and set buffers for next draw
     */
    public void surfaceChanged(SurfaceHolder pHolder, int pFormat, int pW, int pH) {

    	if (pHolder.getSurface() == null) {
    		Log.i(TAG,"No proper holder");
            return;
        }
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        	Log.i(TAG,"tried to stop a non-existent preview");
        	return;
        }
        PixelFormat pxlFrmt = new PixelFormat();
        PixelFormat.getPixelFormatInfo(mCamera.getParameters().getPreviewFormat(), pxlFrmt);
        int srcSize 		= Constants.MAX_DISP_IMG_WIDTH * Constants.MAX_DISP_IMG_HEIGHT * pxlFrmt.bitsPerPixel/8;
        mVideoSource        = new byte[srcSize];
        mBackBuffer         = Bitmap.createBitmap(Constants.MAX_DISP_IMG_WIDTH,
        		Constants.MAX_DISP_IMG_HEIGHT,Bitmap.Config.ARGB_8888);
    	Camera.Parameters camParams = mCamera.getParameters();
    	camParams.setPreviewSize(Constants.MAX_DISP_IMG_WIDTH,
                Constants.MAX_DISP_IMG_HEIGHT);
        camParams.setPreviewFormat(ImageFormat.NV21);
        camParams.setPreviewFpsRange(Constants.MIN_FPS,Constants.MAX_FPS);
        mCamera.setParameters(camParams);

        mImgDims[0] = Constants.MAX_DISP_IMG_WIDTH;
        mImgDims[1] = Constants.MAX_DISP_IMG_HEIGHT;

        mSrcRect	= new Rect(0,0,mImgDims[0],mImgDims[1]);
        mTrgtRect	= pHolder.getSurfaceFrame();

        mCamera.addCallbackBuffer(mVideoSource);

        try {
            mCamera.setPreviewDisplay(pHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "@SurfaceChanged:Error starting camera preview: " + e.getMessage());
        }
    }

    /*
     * Sets the processing filter to be used
     * 0: NV21 to RGB8888
     * 1: Laplacian filter 
     */
    public void setProcessedPreview(int choice) {
    	mSelFilter = choice;
        if (choice==0)
            mDisplayStr = "RGBA";
        else if (choice==1)
            mDisplayStr = "Laplacian";
        else if (choice==2)
            mDisplayStr = "Gaussian";
    }

    /*
     * 
     */
    public void onPreviewFrame(byte[] data, Camera camera) {
    	try {
            runfilter(mBackBuffer,data,mImgDims[0],mImgDims[1],mSelFilter);
    	} catch(Exception e) {
    		Log.i(TAG, e.getMessage());
    	}
        invalidate();	// Trigger draw command
    }

    /*
     * Draw processed image from buffer and draw text above it 
     */
    @Override
    protected void onDraw(Canvas pCanvas) {
        if( mCamera != null ) {
            if( mBackBuffer!=null ) {
            	pCanvas.drawBitmap(mBackBuffer, mSrcRect, mTrgtRect, null);
            	pCanvas.drawText(mDisplayStr, 64, 64, mPaint);
            }
            // Set the camera buffer
			mCamera.addCallbackBuffer(mVideoSource);  
        }
    }

    /*
     * Release the camera object created internally
     */
	private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();
            mCamera = null;
        }
    }

	/*
	 * On destroy stop the camera feedback and destroy it
	 * "Release" the image buffer
	 */
    public void surfaceDestroyed(SurfaceHolder holder) {
    	if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            releaseCamera();
            mVideoSource = null;
            mBackBuffer = null;
        }
    }

}