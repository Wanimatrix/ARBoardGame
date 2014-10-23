package be.wouterfranken.arboardgame.app;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import be.wouterfranken.arboardgame.rendering.CameraViewRenderer;

import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.CardboardView;

public class CameraView extends CardboardView implements Callback{
//	private Camera camera;
//	private boolean inPreview;
	private CameraViewRenderer renderer;
	private final static String TAG = CameraView.class.getSimpleName();
	
	public CameraView(Context context) {
		this(context,null);
	}
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		renderer = new CameraViewRenderer(this);
//		this.setFovY(33.242531f);
		setEGLContextClientVersion(2);
		setRenderer(renderer);
		
//		setVRModeEnabled(false);
//		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setDistortionCorrectionEnabled(true);
//		getHolder().addCallback(this);
//		inPreview = false;
	}
	
//	public boolean isInPreview() {
//		return inPreview;
//	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		renderer.getCameraRenderer().stopCamera();
		super.surfaceDestroyed(holder);
//		if(inPreview) {
//			camera.stopPreview();
//		}
//		
//		camera.release();
//		camera = null;
//		inPreview = false;
	}
	
	/**
	 * Set cameraPreview in surfaceHolder after surface was created.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		renderer.getCameraRenderer().startCamera();
		super.surfaceCreated(holder);
	}
	
	/**
	 * When surface has change we can determine the best preview size,
	 * set the camera parameters to this size and start the preview.
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w,
			int h) {
		super.surfaceChanged(holder, format, w, h);
//		Camera.Parameters params = camera.getParameters();
//		Camera.Size size = getBestPreviewSize(width, height, params);
//		
//		try {
//			camera.setPreviewDisplay(holder);
//		} catch (IOException e) {
//			Log.e("CameraView", "Exception in setPreviewDisplay()", e);
//		}
//		
//		if(size != null) {
//			params.setPreviewSize(size.width, size.height);
//			camera.setParameters(params);
//			camera.startPreview();
//			inPreview = true;
//		}
		Log.d(TAG, "Width: "+w+",Height: "+h);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d(TAG, "Touch noticed");
		AppConfig.TOUCH_EVENT = true;
		Log.d(TAG,"STL: "+getHeadMountedDisplay().getCardboard().getScreenToLensDistance());
		return super.onTouchEvent(event);
	}
	
//	/**
//	 * Returns the best supported preview size, based on the given width and height.
//	 * 
//	 * The best preview size is defined as the one that: 
//	 * 		- fits within given width and height;
//	 * 		- has the biggest area.
//	 */
//	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters params) {
//		Camera.Size result = null;
//		
//		for(Camera.Size size : params.getSupportedPreviewSizes()) {
//			if(size.width<= width && size.height <= height) {
//				if(result == null) {
//					result = size;
//				} else {
//					int resultArea = result.width*result.height;
//					int newArea = size.width*size.height;
//					
//					if(newArea > resultArea)
//						result = size;
//				}
//			}
//		}
//		
//		return result;
//	}

}
