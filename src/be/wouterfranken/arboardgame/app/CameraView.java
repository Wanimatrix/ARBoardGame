package be.wouterfranken.arboardgame.app;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.ViewConfiguration;
import android.view.Window;
import be.wouterfranken.arboardgame.rendering.ArRenderer;
import be.wouterfranken.arboardgame.rendering.CameraViewRenderer;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;

import com.google.vrtoolkit.cardboard.CardboardView;

public class CameraView extends CardboardView implements Callback{
	private final static String TAG = CameraView.class.getSimpleName();
	
	private CameraViewRenderer renderer;
	private GamePhaseManager gpManager;
	
	private boolean distortionCorrectionEnabled = false;
	
	public CameraView(Context context) {
		this(context,null);
	}
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		CameraPoseTracker cpTracker = new CameraPoseTracker(getContext());
		gpManager = new GamePhaseManager(cpTracker, this);
		renderer = new CameraViewRenderer(this, cpTracker, gpManager);
		gpManager.setAr(renderer.getArRenderer());
		setEGLContextClientVersion(3);
		setRenderer(renderer);
		
		
		setVRModeEnabled(false);
//		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setDistortionCorrectionEnabled(false);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		renderer.getArRenderer().stopCamera();
		super.surfaceDestroyed(holder);
	}
	
	/**
	 * Set cameraPreview in surfaceHolder after surface was created.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		renderer.getArRenderer().startCamera();
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
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Width: "+w+",Height: "+h);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d(TAG, "Downtime: "+(event.getEventTime()-event.getDownTime())+" < "+ViewConfiguration.getLongPressTimeout());
		if(event.getAction() == MotionEvent.ACTION_UP && event.getEventTime()-event.getDownTime() < ViewConfiguration.getLongPressTimeout()) {
			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Touch noticed!");
			gpManager.goToNextPhase();
//			GamePhaseManager.distanceCollect.save("/sdcard/arbg/distances", "");
			AppConfig.TOUCH_EVENT = true;
		}
		return super.onTouchEvent(event);
	}
	
	@Override
	public void setVRModeEnabled(boolean enabled) {
		super.setVRModeEnabled(enabled);
		distortionCorrectionEnabled = enabled;
		renderer.getFinalRenderer().setVRModeEnabled(enabled);
	};
	
	public void onFinishFrame() {
		setDistortionCorrectionEnabled(distortionCorrectionEnabled);
	}
	
	public GamePhaseManager getGamePhaseManager() {
		return gpManager;
	}
}
