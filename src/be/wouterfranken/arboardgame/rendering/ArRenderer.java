package be.wouterfranken.arboardgame.rendering;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.R;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.FullSquadMesh;
import be.wouterfranken.arboardgame.rendering.meshes.GameBoardOverlayMesh;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPose;
import be.wouterfranken.arboardgame.rendering.tracking.FrameTrackingCallback;
import be.wouterfranken.arboardgame.rendering.tracking.LegoBrick;
import be.wouterfranken.arboardgame.utilities.AndroidUtils;
import be.wouterfranken.arboardgame.utilities.RenderingUtils;
import be.wouterfranken.arboardgame.utilities.TrackingUtilities;

public class ArRenderer implements Renderer, PreviewCallback {
	
	private final static String TAG = ArRenderer.class.getSimpleName();
	
	
	private Camera camera;
	private int[] tex;
	private SurfaceTexture st;
	private Buffer pTexCoordVBG;
	private Buffer pVertexVBG;
	
	private CameraPose cameraPose;
	private LegoBrick legoBrick;
	private Context context;
	
    private float[] mvp = new float[16];
    private float[] glProj = null;
    private float[] glMv = null;
    
    private GameBoardOverlayMesh gbo = new GameBoardOverlayMesh(new float[]{20f,20f});
    
    
    // RENDER TO TEXTURE VARIABLES
 	int[] fb, depthRb, renderTex; // the framebuffer, the renderbuffer and the texture to render
 	int texW = AppConfig.PREVIEW_RESOLUTION[0];           // the texture's width
 	int texH = AppConfig.PREVIEW_RESOLUTION[1];           // the texture's height
 	IntBuffer texBuffer;          //  Buffer to store the texture
 	
 	// RENDERING HANDLERS
 	private int[] programId;
 	private int vTexCoordHandler;
	private int sTextureHandler;
	private int[] vPositionHandler;
    private int[] mvpHandler;
    private int texColorHandler;
	
    // SHADERS
	private final String vss =
		"attribute vec3 vPosition;\n" +
		"uniform mat4 u_MVP;\n" +
		"void main() {\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, 0, 1.0 );\n" +
		"}";
 
	private final String fss =
		"precision mediump float;\n" +
		"uniform vec4 color;\n" +
		"void main() {\n" +
		"  gl_FragColor = color;\n" +
		"}";
    
	private final String vssCamera =
		"attribute vec2 vPosition;\n" +
		"attribute vec2 vTexCoord;\n" +
		"uniform mat4 u_MVP;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  texCoord = vTexCoord;\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
		"}";
 
	private final String fssCamera =
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  gl_FragColor = texture2D(sTexture,texCoord);\n" +
		"}";
	
	public ArRenderer(Context ctx, CameraPose cameraPose) {
		this.cameraPose = cameraPose;
		this.legoBrick = new LegoBrick();
		this.context = ctx;
		
		
		
	}
	
	/**
	 ***********
	 * GETTERS *
	 ***********
	 */
	
	public Camera getCamera() {
		return camera;
	}
	
	public int getTextureId() {
		return renderTex[0];
	}

	/**
	 ****************************
	 * CAMERA CONTROL FUNCTIONS *
	 ****************************
	 */
	
	public void startCamera() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting camera...");
		
		if(camera == null) {
			camera = Camera.open();
		}
		try {
			camera.setPreviewTexture(st);
		} catch (IOException e) {
		}
	}

	public void stopCamera() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Stopping camera...");
		
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}

	/**
	 **************************
	 * SURFACE INIT FUNCTIONS *
	 **************************
	 */
	
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		initFrameBuffer();
		
		setupCameraTex();
		
		GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		// Backface Culling
	//	GLES20.glDisable(GLES20.GL_CULL_FACE);
		
		setupRenderHandlers();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting vertices, texcoords, camerapose init...");	
		
		FullSquadMesh squad = new FullSquadMesh();
		
		pTexCoordVBG = squad.getTexCoords();
		pVertexVBG = squad.getVertices();
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Vertices, texcoords, camerapose init done...");
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting camera setup...");
		
	    setupCamera();
		
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Camera setup done...");
	}

	/**
	 ******************************
	 * RENDERER CONTROL FUNCTIONS *
	 ******************************
	 */
	
	@Override
	public void onDrawFrame(GL10 unused) {
		st.updateTexImage();
		
		GLES20.glViewport(0, 0, this.texW, this.texH);
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
     	GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
     	GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		renderCamera();
	    renderVirtualLayer();
	    
	    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
	}

	private void renderVirtualLayer() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Trying to get Modelview and Projection for virtual layer...");
	    double[] mv = cameraPose.getMv();
		double[] proj = cameraPose.getProj();
		if(mv != null && proj != null) {
			glMv = new float[16];
			glProj = new float[16];
			
			for (int i = 0; i < mv.length; i++) {
				glProj[i] = (float) proj[i];
				glMv[i] = (float) mv[i];
			}
			
			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Getting Modelview and Projection for virtual layer successful");
		} else {
			glProj = null;
			glMv = null;
			
			if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Getting Modelview and Projection for virtual layer failed (no marker in sight?)");
		} 
	    
		 // Render virtual layer to texture
		GLES20.glUseProgram(programId[1]);
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting rendering virtual layer to FrameBuffer...");
		
	    if(glProj != null && glMv != null) {
	    	
	    	
		    // Render virtual layer to texture
			GLES20.glUseProgram(programId[1]);
			GLES20.glEnable(GLES20.GL_BLEND);
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		    
			// GAMEBOARD
		    GLES20.glVertexAttribPointer(vPositionHandler[1], 4, GLES20.GL_FLOAT, false, 4*3, gbo.getVertices());
		    GLES20.glEnableVertexAttribArray(vPositionHandler[1]);
		    GLES20.glUniform4f(texColorHandler, 1, 0, 0, 0.5f);
		    
		    Matrix.multiplyMM(mvp, 0, glProj, 0, glMv, 0);
		    Matrix.scaleM(mvp, 0, 1, -1, 1);
	        
		    GLES20.glUniformMatrix4fv(mvpHandler[1], 1, false, mvp, 0);
		  
		    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		    GLES20.glFlush();
		    
		    // GBOCENTER X
		    GameBoardOverlayMesh gboX = new GameBoardOverlayMesh(new float[]{1.6f,20f});
		    GLES20.glVertexAttribPointer(vPositionHandler[1], 4, GLES20.GL_FLOAT, false, 4*3, gboX.getVertices());
		    GLES20.glEnableVertexAttribArray(vPositionHandler[1]);
		    GLES20.glUniform4f(texColorHandler, 0, 1, 0, 0.5f);
		    
		    Matrix.multiplyMM(mvp, 0, glProj, 0, glMv, 0);
		    Matrix.scaleM(mvp, 0, 1, -1, 1);
	        
		    GLES20.glUniformMatrix4fv(mvpHandler[1], 1, false, mvp, 0);
		  
		    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		    GLES20.glFlush();
		    
		    // GBOCENTER X
		    GameBoardOverlayMesh gboY = new GameBoardOverlayMesh(new float[]{20f,1.6f});
		    GLES20.glVertexAttribPointer(vPositionHandler[1], 4, GLES20.GL_FLOAT, false, 4*3, gboY.getVertices());
		    GLES20.glEnableVertexAttribArray(vPositionHandler[1]);
		    GLES20.glUniform4f(texColorHandler, 0, 0, 1, 0.5f);
		    
		    Matrix.multiplyMM(mvp, 0, glProj, 0, glMv, 0);
		    Matrix.scaleM(mvp, 0, 1, -1, 1);
	        
		    GLES20.glUniformMatrix4fv(mvpHandler[1], 1, false, mvp, 0);
		  
		    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		    GLES20.glFlush();
		    
		    
	    }
	    
	    float[][] contours = legoBrick.getGLContour();
	    if(contours != null) {
	    	// LegoBrick Contour
	    	int totalContourCount = 0;
	    	for (int i = 0; i < contours.length; i++) {
				totalContourCount+= contours[i].length;
			}
    		ByteBuffer bb = ByteBuffer.allocateDirect(4 * totalContourCount);
	        bb.order(ByteOrder.nativeOrder());
	        for (float[] tmp : contours) {
	        	for (float d : tmp) {
	        		bb.putFloat(d);
	        	}
	        }
	        bb.rewind();
		    GLES20.glVertexAttribPointer(vPositionHandler[1], 3, GLES20.GL_FLOAT, false, 4*3, bb);
		    GLES20.glEnableVertexAttribArray(vPositionHandler[1]);
		    GLES20.glUniform4f(texColorHandler, 0, 1, 1, 1f);
		    
		    Matrix.setIdentityM(mvp, 0);
//			    Matrix.scaleM(mvp, 0, 1, -1, 1);
		    
		    GLES20.glUniformMatrix4fv(mvpHandler[1], 1, false, mvp, 0);
		    for (int j = 0; j < contours.length; j++) {
		    	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, j==0 ? 0 : contours[j-1].length/3, contours[j].length/3);
		    	GLES20.glFlush();
			}
		    GLES20.glDisableVertexAttribArray(vPositionHandler[1]);
	    }
	    
	    GLES20.glDisable(GLES20.GL_BLEND);
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Rendering virtual layer to FrameBuffer done...");
	}

	private void renderCamera() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting rendering camera to FrameBuffer...");
		
		// Render camera to texture
		GLES20.glUseProgram(programId[0]);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
	    GLES20.glUniform1i(sTextureHandler, 0);
	    
	    GLES20.glVertexAttribPointer(vPositionHandler[0], 2, GLES20.GL_FLOAT, false, 4*2, pVertexVBG);
	    GLES20.glEnableVertexAttribArray(vPositionHandler[0]);
	    GLES20.glVertexAttribPointer(vTexCoordHandler, 2, GLES20.GL_FLOAT, false, 4*2, pTexCoordVBG );
	    GLES20.glEnableVertexAttribArray(vTexCoordHandler);
	    
	    Matrix.setIdentityM(mvp, 0);
	    Matrix.scaleM(mvp, 0, 1, -1, 1);
	    
	    GLES20.glUniformMatrix4fv(mvpHandler[0], 1, false, mvp, 0);
	
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	    GLES20.glFlush();
	    
	    GLES20.glDisableVertexAttribArray(vPositionHandler[0]);
	    GLES20.glDisableVertexAttribArray(vTexCoordHandler);
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Rendering camera to FrameBuffer done...");
	}

	/**
	 **************************
	 *     OnPreviewFrame     *
	 **************************
	 */
	
	@Override
	public void onPreviewFrame(byte[] frameData, Camera camera) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Updating camera pose ...");
		
		long start = System.nanoTime();
		Size size = camera.getParameters().getPreviewSize();
		long start2 = System.nanoTime();
		Mat colFrameImg = new Mat();
		Mat yuv = new Mat( (int)(size.height*1.5), size.width, CvType.CV_8UC1 );
		yuv.put( 0, 0, frameData );
		Imgproc.cvtColor( yuv, colFrameImg, Imgproc.COLOR_YUV2BGR_NV21, 3);
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "YUV2RGB (OpenCV) in "+(System.nanoTime()-start2)/1000000L+"ms");
		
		FrameTrackingCallback callback = new FrameTrackingCallback(frameData, camera);
		if(AppConfig.CAMERA_POSE_ESTIMATION) cameraPose.updateCameraPose(colFrameImg, callback);
		if(AppConfig.LEGO_TRACKING) legoBrick.findLegoBrick(colFrameImg, callback);
		
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Totaltime in "+(System.nanoTime()-start)/1000000L+"ms");
	}
	
	/**
	 **************************
	 * SETUP HELPER FUNCTIONS *
	 **************************
	 */

	private void setupCamera() {
		// Setup Camera Parameters
	    Camera.Parameters params = camera.getParameters();
	    params.setPreviewFormat(ImageFormat.NV21);
		params.setPreviewFpsRange(AppConfig.FPS_RANGE[0], AppConfig.FPS_RANGE[1]);
		params.setPreviewSize(AppConfig.PREVIEW_RESOLUTION[0], AppConfig.PREVIEW_RESOLUTION[1]);
		params.set("orientation", "landscape");
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		camera.setParameters(params);
		camera.startPreview();
		
		// Add callback buffers to camera for frame handling
		float bytesPerPix = ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8.0f;
		int frame_byteSize = (int) ((AppConfig.PREVIEW_RESOLUTION[0] * AppConfig.PREVIEW_RESOLUTION[1]) * bytesPerPix);
		
		for(int i = 0; i< AppConfig.AMOUNT_PREVIEW_BUFFERS ; i++) {
			camera.addCallbackBuffer(new byte[frame_byteSize]);
		}
		
		camera.setPreviewCallbackWithBuffer(this);
	}

	private void setupRenderHandlers() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting renderhandlers init...");
		
		programId = new int[2];
		vPositionHandler = new int[2];
		mvpHandler = new int[2];
		
		programId[0] = RenderingUtils.createProgramFromShaderSrc(vssCamera,fssCamera);
		
		vPositionHandler[0] = GLES20.glGetAttribLocation(programId[0], "vPosition");
	    vTexCoordHandler = GLES20.glGetAttribLocation ( programId[0], "vTexCoord" );
	    sTextureHandler = GLES20.glGetUniformLocation ( programId[0], "sTexture" );
	    mvpHandler[0] = GLES20.glGetUniformLocation(programId[0], "u_MVP");
	    
	    programId[1] = RenderingUtils.createProgramFromShaderSrc(vss,fss);
	    vPositionHandler[1] = GLES20.glGetAttribLocation(programId[1], "vPosition");
	    mvpHandler[1] = GLES20.glGetUniformLocation(programId[1], "u_MVP");
	    texColorHandler = GLES20.glGetUniformLocation(programId[1], "color");
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Renderhandlers init done...");
	}

	private void setupCameraTex() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting camera texture setup...");
		
		tex = new int[2];
		GLES20.glGenTextures(1, tex, 0);
	    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		st = new SurfaceTexture(tex[0]);
//		st.setOnFrameAvailableListener(this);
		
		startCamera();
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Camera texture setup done...");
	}

	private void initFrameBuffer() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting FrameBuffer init...");
		
		// create the ints for the framebuffer, depth render buffer and texture
        fb = new int[1];
        depthRb = new int[1];
        renderTex = new int[1];
         
        // generate
        GLES20.glGenFramebuffers(1, fb, 0);
        GLES20.glGenRenderbuffers(1, depthRb, 0); // the depth buffer
        GLES20.glGenTextures(1, renderTex, 0);
         
        // generate texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);
         
        // parameters - we have to make sure we clamp the textures to the edges
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        
        // generate the textures
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texW, texH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
         
        // create render buffer and bind 16-bit depth buffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texW, texH);
        
        // Bind the framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
		 
		// specify texture as color attachment
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTex[0], 0);
		 
		// attach render buffer as depth buffer
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);
		 
		// check status
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
		    Log.e(TAG, "FRAMEBUFFER INCOMPLETE"); 
		    System.exit(1);
		}
		
		// Bind screen as framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "FrameBuffer init successful...");
	}
}
