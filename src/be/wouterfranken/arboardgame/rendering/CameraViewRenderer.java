package be.wouterfranken.arboardgame.rendering;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;
import be.wouterfranken.arboardgame.app.CameraView;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPose;

import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class CameraViewRenderer implements StereoRenderer{
	
	private static final String TAG = CameraViewRenderer.class.getSimpleName();
	
	private CameraRenderer cameraRenderer;
	private VirtualLayerRenderer virtualLayerRenderer;
	private CameraView view;
	
	public CameraViewRenderer(CameraView view) {
		this.view = view;
		cameraRenderer = new CameraRenderer(view);
		virtualLayerRenderer = new VirtualLayerRenderer(new CameraPose(this.view.getContext()), cameraRenderer.getCamera(), view.getContext());
		
	}
	
	@Override
	public void onDrawEye(EyeTransform transform) {
		
		if(cameraRenderer.getCamera() != null) {
//			transform.getParams().getFov().setLeft((transform.getParams().getFov().getLeft()+transform.getParams().getFov().getRight())/2);
//			transform.getParams().getFov().setRight((transform.getParams().getFov().getLeft()+transform.getParams().getFov().getRight())/2);
//			transform.getParams().getFov().setBottom((transform.getParams().getFov().getTop()+transform.getParams().getFov().getBottom())/2);
//			transform.getParams().getFov().setTop((transform.getParams().getFov().getTop()+transform.getParams().getFov().getBottom())/2);
			
//			transform.getParams().getFov().setLeft(45);
//			transform.getParams().getFov().setRight(45);
//			transform.getParams().getFov().setBottom(45);
//			transform.getParams().getFov().setTop(45);
			
			cameraRenderer.onDrawEye(transform);
			virtualLayerRenderer.onDrawEye(transform);
		}
	}

	@Override
	public void onSurfaceChanged(int width, int height) {
		if(cameraRenderer.getCamera() != null) {
			cameraRenderer.onSurfaceChanged(width, height);
			virtualLayerRenderer.onSurfaceChanged(width, height);
			
			cameraRenderer.getCamera().setPreviewCallbackWithBuffer(virtualLayerRenderer);
		} else {
			Log.w(TAG, "Preview Callback not attached");
		}
	}

	@Override
	public void onSurfaceCreated(EGLConfig config) {
		if(cameraRenderer.getCamera() != null) {
			cameraRenderer.onSurfaceCreated(config);
			virtualLayerRenderer.onSurfaceCreated(config);
		}
	}

	@Override
	public void onFinishFrame(Viewport vp) {
		if(cameraRenderer.getCamera() != null) {
			cameraRenderer.onFinishFrame(vp);
			virtualLayerRenderer.onFinishFrame(vp);
		}
	}

	@Override
	public void onNewFrame(HeadTransform ht) {
		if(cameraRenderer.getCamera() != null) {
			cameraRenderer.onNewFrame(ht);
			virtualLayerRenderer.onNewFrame(ht);
		}
	}

	@Override
	public void onRendererShutdown() {
		cameraRenderer.onRendererShutdown();
		virtualLayerRenderer.onRendererShutdown();
	}
	
	public CameraRenderer getCameraRenderer() {
		return cameraRenderer;
	}
	
	public VirtualLayerRenderer getVirtualLayerRenderer() {
		return virtualLayerRenderer;
	}
	
}
