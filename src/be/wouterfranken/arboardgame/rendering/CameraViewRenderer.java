package be.wouterfranken.arboardgame.rendering;

import javax.microedition.khronos.egl.EGLConfig;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.app.CameraView;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;

import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class CameraViewRenderer implements StereoRenderer{
	
	private static final String TAG = CameraViewRenderer.class.getSimpleName();
	
	private FinalRenderer finalRenderer;
	private ArRenderer arRenderer;
	private CameraView view;
	private long renderDataTimer;
	
	public CameraViewRenderer(CameraView view) {
		this.view = view;
		arRenderer = new ArRenderer(this.view.getContext(),new CameraPoseTracker(this.view.getContext()));
		finalRenderer = new FinalRenderer(view);
	}
	
	/**
	 ***********
	 * GETTERS *
	 ***********
	 */
	
	public FinalRenderer getFinalRenderer() {
		return finalRenderer;
	}

	public ArRenderer getArRenderer() {
		return arRenderer;
	}

	/**
	 **************************
	 * SURFACE INIT FUNCTIONS *
	 **************************
	 */
	
	@Override
	public void onSurfaceCreated(EGLConfig config) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "onSurfaceCreated");
		
		if(arRenderer.getCamera() != null) {
			arRenderer.onSurfaceCreated(null,config);
			finalRenderer.setTexHandler(arRenderer.getTextureId());
			finalRenderer.onSurfaceCreated(config);
		}
	}

	@Override
	public void onSurfaceChanged(int width, int height) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "onSurfaceChanged");
		
		if(arRenderer.getCamera() != null) {
			arRenderer.onSurfaceChanged(null, width, height);
			finalRenderer.onSurfaceChanged(width, height);
		} else {
			Log.e(TAG, "Preview Callback not attached");
		}
	}
	
	/**
	 ******************************
	 * RENDERER CONTROL FUNCTIONS *
	 ******************************
	 */

	@Override
	public void onNewFrame(HeadTransform ht) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "onNewFrame");
		renderDataTimer = System.currentTimeMillis();
		if(arRenderer.getCamera() != null) {
			arRenderer.onDrawFrame(null);
			finalRenderer.onNewFrame(ht);
		}
	}

	@Override
	public void onDrawEye(EyeTransform transform) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "OnDrawEye");
		
		if(arRenderer.getCamera() != null) {
			finalRenderer.onDrawEye(transform);
		}
	}

	@Override
	public void onFinishFrame(Viewport vp) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "onFinishFrame");
		
		if(arRenderer.getCamera() != null) {
			finalRenderer.onFinishFrame(vp);
		}
		
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Rendering done in "+(System.currentTimeMillis()-renderDataTimer)+"ms");
	}

	@Override
	public void onRendererShutdown() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "onRendererShutdown");
		
		finalRenderer.onRendererShutdown();
	}
	
}
