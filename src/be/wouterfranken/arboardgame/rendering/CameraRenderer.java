package be.wouterfranken.arboardgame.rendering;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.app.CameraView;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.RenderingUtils;

import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.EyeParams.Eye;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class CameraRenderer implements StereoRenderer, OnFrameAvailableListener {
	
	private static final String TAG = CameraRenderer.class.getSimpleName();
	
	private Camera camera;
	private int[] tex;
	private SurfaceTexture st;
	private int programId;
	private CameraView view;
	private FloatBuffer pVertex;
	private FloatBuffer pTexCoordRight;
	private FloatBuffer pTexCoordLeft;
	
	private final String vss =
		"attribute vec2 vPosition;\n" +
		"attribute vec2 vTexCoord;\n" +
		"uniform mat4 u_MVP;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  texCoord = vTexCoord;\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
		"}";
 
	private final String fss =
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  gl_FragColor = texture2D(sTexture,texCoord);\n" +
		"}";
	
	private float[] mv = new float[16];
    private float[] mvp = new float[16];
	
	private int vPositionHandler;
	private int vTexCoordHandler;
	private int sTextureHandler;
	private int mvpHandler;
	
	public CameraRenderer(CameraView view) {
		this.view = view;
	    Matrix.setIdentityM(mv, 0);
	    
	    Matrix.translateM(mv, 0, 0, 0, -3.4f);
	    Matrix.scaleM(mv, 0, 1f, AppConfig.ASPECT_RATIO, 1f);
	}

	@Override
	public void onDrawEye(EyeTransform transform) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glUseProgram(programId);
	    
	    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
	    GLES20.glUniform1i(sTextureHandler, 0);
	    
	    GLES20.glVertexAttribPointer(vPositionHandler, 2, GLES20.GL_FLOAT, false, 4*2, pVertex);
	    GLES20.glEnableVertexAttribArray(vPositionHandler);
	    if (transform.getParams().getEye() == Eye.LEFT)
	    	GLES20.glVertexAttribPointer(vTexCoordHandler, 2, GLES20.GL_FLOAT, false, 4*2, pTexCoordRight );
	    else
	    	GLES20.glVertexAttribPointer(vTexCoordHandler, 2, GLES20.GL_FLOAT, false, 4*2, pTexCoordLeft );
	    GLES20.glEnableVertexAttribArray(vTexCoordHandler);
	    
        Matrix.multiplyMM(mvp, 0, transform.getPerspective(), 0,
                mv, 0);
        
	    GLES20.glUniformMatrix4fv(mvpHandler, 1, false, mvp, 0);
	  
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	    GLES20.glFlush();
	}
	
	@Override
	public void onFinishFrame(Viewport arg0) {
	}

	@Override
	public void onNewFrame(HeadTransform arg0) {
		st.updateTexImage();
	}

	@Override
	public void onRendererShutdown() {
	}

	@Override
	public void onSurfaceChanged(int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		
		float[] ttmp = { 
				1.0f, 1.0f, 
				0.0f, 1.0f, 
				1.0f, 0.0f, 
				0.0f, 0.0f };

		
		// Setup vertex and texcoords for cameraPreviewTexture
	    pTexCoordRight = ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	    pTexCoordRight.put ( ttmp );
	    pTexCoordRight.position(0);

		pTexCoordLeft = ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		pTexCoordLeft.put ( ttmp );
		pTexCoordLeft.position(0);
		
		// Fill the texture
		float[] vtmp = {
				1f, -1f,
	            -1f, -1f,
	            1f, 1f,
	            -1f, 1f,
	    };
	    pVertex = ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	    pVertex.put ( vtmp );
	    pVertex.position(0);
		
	    // Setup Camera Parameters
	    Camera.Parameters params = camera.getParameters();
		params.setPreviewFpsRange(AppConfig.FPS_RANGE[0], AppConfig.FPS_RANGE[1]);
		params.setPreviewSize(AppConfig.PREVIEW_RESOLUTION[0], AppConfig.PREVIEW_RESOLUTION[1]);
		params.set("orientation", "landscape");
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		camera.setParameters(params);
		camera.startPreview();
		
		// Add callback buffers to camera for frame handling
		float bytesPerPix = ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8.0f;
		int frame_byteSize = (int) ((AppConfig.PREVIEW_RESOLUTION[0] * AppConfig.PREVIEW_RESOLUTION[1]) * bytesPerPix);
		
		for(int i = 0; i< AppConfig.AMOUNT_PREVIEW_BUFFERS ; i++) {
			camera.addCallbackBuffer(new byte[frame_byteSize]);
		}
	}

	@Override
	public void onSurfaceCreated(EGLConfig arg0) {
		tex = new int[2];
		GLES20.glGenTextures(1, tex, 0);
	    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		st = new SurfaceTexture(tex[0]);
		st.setOnFrameAvailableListener(this);
		
		startCamera();
		
		GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		// Backface Culling
	//	GLES20.glDisable(GLES20.GL_CULL_FACE);
		
		programId = RenderingUtils.createProgramFromShaderSrc(vss,fss);
		
		vPositionHandler = GLES20.glGetAttribLocation(programId, "vPosition");
	    vTexCoordHandler = GLES20.glGetAttribLocation ( programId, "vTexCoord" );
	    sTextureHandler = GLES20.glGetUniformLocation ( programId, "sTexture" );
	    mvpHandler = GLES20.glGetUniformLocation(programId, "u_MVP");
	}

	/**
	 * Returns the best supported preview size, based on the given width and height.
	 * 
	 * The best preview size is defined as the one that: 
	 * 		- fits within given width and height;
	 * 		- has the biggest area.
	 */
//	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters params) {
//		Camera.Size result = null;
//		
//		List<Size> previewSizes = params.getSupportedPreviewSizes();
//		Log.d(TAG, "Supported resolutions: ");
//		for (int i = 0; i < previewSizes.size(); i++) {
//			Log.d(TAG, "("+previewSizes.get(i).width+","+previewSizes.get(i).height+")");
//		}
//		
//		for(Camera.Size size : params.getSupportedPreviewSizes()) {
//			if(size.width == 640 && size.height == 480) { // take highest resolution
//				result = size;
//				break;
//			}
//			if(size.width <= width && size.height <= height) {
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
//		Log.d(TAG, "Resolution: ("+result.width+","+result.height+")");
//		return result;
//	}
	
	public void stopCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}
	
	public void startCamera() {
		if(camera == null) {
			camera = Camera.open();
		}
		try {
			camera.setPreviewTexture(st);
		} catch (IOException e) {
		}
	}
	
	public Camera getCamera() {
		return camera;
	}

	@Override
	public void onFrameAvailable(SurfaceTexture s) {
//		view.requestRender();
	}
}
