package be.wouterfranken.arboardgame.rendering;

import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.LegoBrick;
import be.wouterfranken.arboardgame.gameworld.LemmingsGenerator;
import be.wouterfranken.arboardgame.gameworld.WorldConfig;
import be.wouterfranken.arboardgame.rendering.meshes.CuboidMesh;
import be.wouterfranken.arboardgame.rendering.meshes.FullSquadMesh;
import be.wouterfranken.arboardgame.rendering.meshes.GameBoardOverlayMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject.BUFFER_TYPE;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;
import be.wouterfranken.arboardgame.rendering.tracking.FrameTrackingCallback;
import be.wouterfranken.arboardgame.rendering.tracking.LegoBrickTracker;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.MathUtilities;
import be.wouterfranken.arboardgame.utilities.RenderingUtils;
import be.wouterfranken.experiments.NumberCollection;
import be.wouterfranken.experiments.TimerManager;

public class ArRenderer implements Renderer, PreviewCallback {
	
	private final static String TAG = ArRenderer.class.getSimpleName();
	private List<MeshObject> meshesToRender = new ArrayList<MeshObject>();
	private GLSurfaceView view;
	private Context context;
	
	private Camera camera;
	private int[] tex;
	private SurfaceTexture st;
	private Buffer pTexCoordVBG;
	private Buffer pVertexVBG;
	
	private CameraPoseTracker cameraPose;
	private LegoBrickTracker legoBrick;
	
    private float[] mvp = new float[16];
    private float[] glProj = null;
    private float[] glMv = null;
    
    private LemmingsGenerator lemmingsGenerator;
    private List<MeshObject> lemmingMeshesToRender = new ArrayList<MeshObject>();
    private Object lock = new Object();
    
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
    private int[] texColorHandler;
	
    // CAMERA SHADERS
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
	
	public ArRenderer(GLSurfaceView view, CameraPoseTracker cameraPose) {
		this.cameraPose = cameraPose;
		this.legoBrick = new LegoBrickTracker();
		this.view = view;
		
		// Add here the meshes that are needed for rendering.
//		GameBoardOverlayMesh gbo = new GameBoardOverlayMesh(new float[]{AppConfig.BOARD_SIZE[0],AppConfig.BOARD_SIZE[1]}, new RenderOptions(true, new Color(1,0,0,0.5f)));
//		meshesToRender.add(gbo);
		if(AppConfig.LEMMING_RENDERING) {
			CuboidMesh startPole = new CuboidMesh(0.5f, 0.5f, 10, WorldConfig.STARTPOINT.x, WorldConfig.STARTPOINT.y, 5f, new RenderOptions(true, new Color(0, 0, 1, 1f), AppConfig.SHADOW_RENDERING));
			meshesToRender.add(startPole);
			CuboidMesh endPole = new CuboidMesh(0.5f, 0.5f, 10, WorldConfig.ENDPOINT.x, WorldConfig.ENDPOINT.y, 5f, new RenderOptions(true, new Color(0, 0, 1, 1f), AppConfig.SHADOW_RENDERING));
			meshesToRender.add(endPole);
		}
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
		lemmingsGenerator = new LemmingsGenerator(legoBrick, cameraPose);
		
		GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		
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
	    surface = new Surface(st2);
		
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Camera setup done...");
	}

	/**
	 ******************************
	 * RENDERER CONTROL FUNCTIONS *
	 ******************************
	 */
	
	@Override
	public void onDrawFrame(GL10 unused) {
		if(!AppConfig.USE_SAVED_FRAMES) st.updateTexImage();
		else st2.updateTexImage();
		
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
	    glMv = cameraPose.getMv();
		glProj = cameraPose.getProj();
		List<MeshObject> lemmMeshesTmp = new ArrayList<MeshObject>();
		synchronized (lock) {
			lemmMeshesTmp.addAll(lemmingMeshesToRender);
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
		    
			
			
			for (int i = 0; i< meshesToRender.size();i++) {
				renderMesh(meshesToRender.get(i), i);
			}
			
			if(AppConfig.LEMMING_RENDERING) {
				for (int i = 0; i< lemmMeshesTmp.size();i++) {
					renderMesh(lemmMeshesTmp.get(i), meshesToRender.size());
				}
				
				List<MeshObject> starMeshes = lemmingsGenerator.getStarMeshes();
				for (int i = 0; i< starMeshes.size();i++) {
					renderMesh(starMeshes.get(i), meshesToRender.size());
				}
			}
			
			List<MeshObject> brickMeshes = lemmingsGenerator.getActiveBrickMeshes(new RenderOptions(true, new Color(0, 0, 1, 1),false));
			for (MeshObject mesh : brickMeshes) {
				renderMesh(mesh, 1);
			}
		    
	    }
	    
	    // RENDER LEGOBRICK CONTOUR
//	    float[][] contours = legoBrick.getGLContour();
//	    if(contours != null) {
//	    	// LegoBrick Contour
//	    	int totalContourCount = 0;
//	    	for (int i = 0; i < contours.length; i++) {
//				totalContourCount+= contours[i].length;
//			}
//    		ByteBuffer bb = ByteBuffer.allocateDirect(4 * totalContourCount);
//	        bb.order(ByteOrder.nativeOrder());
//	        for (float[] tmp : contours) {
//	        	for (float d : tmp) {
//	        		bb.putFloat(d);
//	        	}
//	        }
//	        bb.rewind();
//		    GLES20.glVertexAttribPointer(vPositionHandler[1], 3, GLES20.GL_FLOAT, false, 4*3, bb);
//		    GLES20.glEnableVertexAttribArray(vPositionHandler[1]);
//		    GLES20.glUniform4f(texColorHandler[0], 0, 1, 0, 1f);
//		    
//		    Matrix.setIdentityM(mvp, 0);
//				    Matrix.scaleM(mvp, 0, 1, -1, 1);
//		    
//		    GLES20.glUniformMatrix4fv(mvpHandler[1], 1, false, mvp, 0);
//		    for (int j = 0; j < contours.length; j++) {
//		    	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, j==0 ? 0 : contours[j-1].length/3, contours[j].length/3);
//		    	GLES20.glFlush();
//			}
//		    GLES20.glDisableVertexAttribArray(vPositionHandler[1]);
//	    }
	    
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
	
	private void renderMesh(MeshObject m, int index) {
		// Backface Culling
		if(m == null) return;
		
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);
		
		RenderOptions ro = m.getRenderOptions();
		int[] multiRenderConfig = m.getMultiRenderConfiguration();
		
		Buffer shadowVerts = m.getShadowVertices();
	    if(shadowVerts != null) {
	    	GLES20.glVertexAttribPointer(vPositionHandler[1+index], 3, GLES20.GL_FLOAT, false, 4*3, shadowVerts);
		    GLES20.glEnableVertexAttribArray(vPositionHandler[1+index]);
		    GLES20.glUniform4f(texColorHandler[index], 0,0,0,0.8f); // 1 0 0 0.5
		    
		    Matrix.setIdentityM(mvp, 0);
		    if(ro.useMVP) Matrix.multiplyMM(mvp, 0, glProj, 0, glMv, 0);
	        
		    GLES20.glUniformMatrix4fv(mvpHandler[1+index], 1, false, mvp, 0);
		    
		    for(int i = 1; i< multiRenderConfig.length+1; i++) {
		    	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, multiRenderConfig[i-1], (i == multiRenderConfig.length ? m.getNumObjectVertex() : multiRenderConfig[i]) - multiRenderConfig[i-1]);
		    }
		    
		    GLES20.glFlush();
		    
		    GLES20.glDisableVertexAttribArray(vPositionHandler[1+index]);
	    }
		
		GLES20.glVertexAttribPointer(vPositionHandler[1+index], 3, GLES20.GL_FLOAT, false, 4*3, m.getVertices());
	    GLES20.glEnableVertexAttribArray(vPositionHandler[1+index]);
	    GLES20.glUniform4f(texColorHandler[index], ro.col.r, ro.col.g, ro.col.b, ro.col.a); // 1 0 0 0.5
	    
	    Matrix.setIdentityM(mvp, 0);
	    if(ro.useMVP) Matrix.multiplyMM(mvp, 0, glProj, 0, glMv, 0);
        
	    GLES20.glUniformMatrix4fv(mvpHandler[1+index], 1, false, mvp, 0);
	    
	    for(int i = 1; i< multiRenderConfig.length+1; i++) {
	    	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, multiRenderConfig[i-1], (i == multiRenderConfig.length ? m.getNumObjectVertex() : multiRenderConfig[i]) - multiRenderConfig[i-1]);
	    }
	    
	    GLES20.glFlush();
	    
	    GLES20.glDisable(GLES20.GL_CULL_FACE);
	}

	/**
	 **************************
	 *     OnPreviewFrame     *
	 **************************
	 */
	
//	int frameCount = 0;
//	long previousFrameTime = 0;
//	int frameWaitAmount = 0;
	private ParallelTask plt = null;
	private int[] distances = new int[]{20,25,30,35,40,45,50,55,60,65,70,75};
	private int currentDistance = 0;
	private int count = 0;
	private Mat currentSavedFrame = new Mat();
	
	private Surface surface;
	public void setSurface(Surface s){
		this.surface = s;
	}
	
	public String getSaveFolderName() {
		String folderName = "/sdcard/arbg/algo";
		if(AppConfig.USE_SAVED_FRAMES) {
			folderName = "/sdcard/arbg/distanceTimingResult/"+distances[currentDistance];
		}
		File f = new File(folderName);
		if(!f.exists()) {
			Log.e(TAG, "Directory "+folderName+"does not exist!");
		}
		Log.d(TAG, "FOLDER NAME: "+folderName);
		return folderName;
	}
	
	@Override
	public void onPreviewFrame(byte[] frameData, Camera camera) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Updating camera pose ...");
		long start = System.nanoTime();
		
		
//		if(previousFrameTime != 0) {
//			if(frameCount == 0) {
//				frameWaitAmount = (int) (15*previousFrameTime);
//			}
//			frameCount = (frameCount + 1) % frameWaitAmount;
//		}
		
//		view.requestRender();
		
		if(AppConfig.USE_SAVED_FRAMES) {
			if(plt != null && plt.getStatus() != AsyncTask.Status.FINISHED && plt.getStatus() != AsyncTask.Status.PENDING) {
				Log.d(TAG, "PLT IS NOT NULL AND NOT FINISHED AND NOT PENDING");
				camera.addCallbackBuffer(frameData);
				return;
			}
			Log.d(TAG, "Loading frame "+"/sdcard/arbg/naiveDistFrames/dist"+distances[currentDistance]+"-"+(count+1)+".png");
			currentSavedFrame = Highgui.imread("/sdcard/arbg/naiveDistFrames/dist"+distances[currentDistance]+"-"+(count+1)+".png",3);
			if(currentSavedFrame.empty()) {
//				if(AppConfig.SAVE_TIMING) {
				TimerManager.saveAll();
				TimerManager.reset();
//				}
				FrameTrackingCallback.unRegister(lemmingsGenerator.getClass());
				lemmingsGenerator = new LemmingsGenerator(legoBrick, cameraPose);
				lemmingMeshesToRender.clear();
				count = 0;
				if(currentDistance < distances.length-1) {
					currentDistance++;
				} else {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
				
				camera.addCallbackBuffer(frameData);
				return;
			}
			Mat tmp = new Mat();
			Paint p = new Paint();
			Imgproc.cvtColor(currentSavedFrame, tmp, Imgproc.COLOR_BGR2RGBA, 4);
			Bitmap bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(tmp, bmp);
			Canvas c = null;
			synchronized (surface) {
				c = surface.lockCanvas(null);
				c.drawBitmap(bmp, new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), new Rect(0, 0, c.getWidth(), c.getHeight()), p);
			}
			surface.unlockCanvasAndPost(c);
		}
		
		
		
		FrameTrackingCallback callback = new FrameTrackingCallback(frameData, camera,start);
		
		if(AppConfig.CAMERA_POSE_ESTIMATION && AppConfig.LEGO_TRACKING && AppConfig.LEMMING_RENDERING
				&& (AppConfig.USE_SAVED_FRAMES || plt == null || plt.getStatus() == AsyncTask.Status.FINISHED || plt.getStatus() == AsyncTask.Status.PENDING)) {
			if(plt != null) Log.d(TAG, "PLT status: "+plt.getStatus());
			plt = new ParallelTask();
			plt.callback = callback;
			plt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frameData);
			if(!AppConfig.USE_SAVED_FRAMES) {
				callback.trackingDone(CameraPoseTracker.class);
				callback.trackingDone(LegoBrickTracker.class);
				callback.trackingDone(LemmingsGenerator.class);
			}
		} else if(AppConfig.CAMERA_POSE_ESTIMATION && AppConfig.LEGO_TRACKING && AppConfig.LEMMING_RENDERING) {
			callback.trackingDone(CameraPoseTracker.class);
			callback.trackingDone(LegoBrickTracker.class);
			callback.trackingDone(LemmingsGenerator.class);
		}

//		if(AppConfig.CAMERA_POSE_ESTIMATION) { 
//			cameraPose.updateCameraPose(colFrameImg, callback);
//			if(!cameraPose.cameraPoseFound()) {
//				camera.addCallbackBuffer(frameData);
//				if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Totaltime in "+(System.nanoTime()-start)/1000000L+"ms");
//			}
//		}
//		if(AppConfig.LEGO_TRACKING && cameraPose.cameraPoseFound()) legoBrick.findLegoBrick(colFrameImg, callback);
//		
//		if(AppConfig.LEMMING_RENDERING && cameraPose.cameraPoseFound()) {
//			long lemmingStart = System.nanoTime();
//			
////			LemmingGeneratorTask lgt = new LemmingGeneratorTask();
////			lgt.setupFrameTrackingCallback(callback);
////			lgt.start = lemmingStart;
////			lgt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//			TimerManager.start("", "getLegobricks", "");
//			LegoBrick[] lbs = legoBrick.getLegoBricks(cameraPose);
//			TimerManager.stop();
//			lemmingsGenerator.frameTick(lbs);
//			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming frameUpdate in "+(System.nanoTime()-lemmingStart)/1000000L+"ms");
//			callback.trackingDone(LemmingsGenerator.class);
//			long lemmingMeshUpd = System.nanoTime();
//			synchronized (lock) {
//				lemmingMeshesToRender.clear();
//				lemmingMeshesToRender.addAll(lemmingsGenerator.getLemmingMeshes());
//			}
////			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming meshes updated in "+(System.nanoTime()-lemmingMeshUpd)/1000000L+"ms");
//		} else if(AppConfig.LEMMING_RENDERING) {
//			callback.trackingDone(LemmingsGenerator.class);
//		}
//		previousFrameTime = (System.nanoTime()-start)/1000000L;
	}
	
	public static NumberCollection distanceCollect = new NumberCollection("distance", "");
	
	private class ParallelTask extends AsyncTask<byte[], Void, Void> {

//		private byte[] frameData = null;
		private FrameTrackingCallback callback = null;
		private boolean touch = false;
		
		@Override
				protected void onPreExecute() {
					touch = AppConfig.TOUCH_EVENT;
					super.onPreExecute();
				}
		
		@Override
		protected Void doInBackground(byte[]... params) {
			TimerManager.start("BrickTracker", "Total", getSaveFolderName());
			if(!AppConfig.USE_SAVED_FRAMES) {
				TimerManager.start("", "frameTicks", getSaveFolderName());
				cameraPose.frameTick();
				legoBrick.frameTick();
				TimerManager.stop();
			}
			
			Mat colFrameImg = new Mat();
			Size size = camera.getParameters().getPreviewSize();
			if(!AppConfig.USE_SAVED_FRAMES) {
				long start2 = System.nanoTime();
				TimerManager.start("Renderer", "yuv2bgr", getSaveFolderName());
				Mat yuv = new Mat( (int)(size.height*1.5), size.width, CvType.CV_8UC1 );
				yuv.put( 0, 0, params[0] );
				Imgproc.cvtColor( yuv, colFrameImg, Imgproc.COLOR_YUV2BGR_NV21, 3);
				if(AppConfig.SAVE_FRAMES && touch) Highgui.imwrite(getSaveFolderName()+"/frames/frame-"+count+".png", colFrameImg);
				TimerManager.stop();
				if(AppConfig.DEBUG_TIMING) Log.d(TAG, "YUV2RGB (OpenCV) in "+(System.nanoTime()-start2)/1000000L+"ms");
			} else {
				colFrameImg = currentSavedFrame.clone();
				Log.d(TAG, "CURRENTFRAME CHANNELS: "+currentSavedFrame.channels());
			}
			
			if(AppConfig.CAMERA_POSE_ESTIMATION) { 
				cameraPose.updateCameraPose(colFrameImg, getSaveFolderName());
				if(!cameraPose.cameraPoseFound()) {
					Log.d(TAG, "Camerapose NOT FOUND");
					if(!AppConfig.USE_SAVED_FRAMES) {
						camera.addCallbackBuffer(params[0]);
					}
					TimerManager.stop();
					return null;
//					if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Totaltime in "+(System.nanoTime()-start)/1000000L+"ms");
				}
			}
			
			double distance = MathUtilities.norm(MathUtilities.vector(new float[]{0,0,0}, cameraPose.getCameraPosition()));
			Log.d(TAG, "Distance to camera: "+distance);
			if(touch) distanceCollect.add(distance);
			
			if(AppConfig.LEGO_TRACKING && cameraPose.cameraPoseFound()) legoBrick.findLegoBrick(colFrameImg, getSaveFolderName());
			
			if(AppConfig.LEMMING_RENDERING && cameraPose.cameraPoseFound()) {
				long lemmingStart = System.nanoTime();
				
//				LemmingGeneratorTask lgt = new LemmingGeneratorTask();
//				lgt.setupFrameTrackingCallback(callback);
//				lgt.start = lemmingStart;
//				lgt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				TimerManager.start("", "getLegobricks", getSaveFolderName());
				LegoBrick[] lbs = legoBrick.getLegoBricks(cameraPose);
				TimerManager.stop();
				lemmingsGenerator.frameTick(lbs, getSaveFolderName());
				if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming frameUpdate in "+(System.nanoTime()-lemmingStart)/1000000L+"ms");
//				callback.trackingDone(LemmingsGenerator.class);
				long lemmingMeshUpd = System.nanoTime();
				synchronized (lock) {
					lemmingMeshesToRender.clear();
					lemmingMeshesToRender.addAll(lemmingsGenerator.getLemmingMeshes());
				}
//				if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming meshes updated in "+(System.nanoTime()-lemmingMeshUpd)/1000000L+"ms");
//			} else if(AppConfig.LEMMING_RENDERING) {
//				callback.trackingDone(LemmingsGenerator.class);
			}
			TimerManager.stop();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			count++;
			if(AppConfig.USE_SAVED_FRAMES) {
				callback.trackingDone(CameraPoseTracker.class);
				callback.trackingDone(LegoBrickTracker.class);
				callback.trackingDone(LemmingsGenerator.class);
			}
			if(touch) AppConfig.TOUCH_EVENT = false;
			super.onPostExecute(result);
		}
		
	}
	
	private class LemmingGeneratorTask extends AsyncTask<Void, Void, Void> {
		private FrameTrackingCallback trackingCallback;
		private long start;
		
		@Override
		protected Void doInBackground(Void... params) {
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming Preprocessing in "+(System.nanoTime()-start)/1000000L+"ms");
			start = System.nanoTime();
			lemmingsGenerator.frameTick(null, getSaveFolderName());//legoBrick.getLegoBricks(cameraPose)
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming frameUpdate in "+(System.nanoTime()-start)/1000000L+"ms");
			this.trackingCallback.trackingDone(LemmingsGenerator.class);
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
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
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
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
		
		programId = new int[2+meshesToRender.size()];
		vPositionHandler = new int[2+meshesToRender.size()];
		mvpHandler = new int[2+meshesToRender.size()];
		texColorHandler = new int[1+meshesToRender.size()];
		
		programId[0] = RenderingUtils.createProgramFromShaderSrc(vssCamera,fssCamera);
		
		vPositionHandler[0] = GLES20.glGetAttribLocation(programId[0], "vPosition");
	    vTexCoordHandler = GLES20.glGetAttribLocation ( programId[0], "vTexCoord" );
	    sTextureHandler = GLES20.glGetUniformLocation ( programId[0], "sTexture" );
	    mvpHandler[0] = GLES20.glGetUniformLocation(programId[0], "u_MVP");
	    
	    for(int i = 0; i<meshesToRender.size();i++) {
	    	MeshObject m = meshesToRender.get(i);
	    	programId[1+i] = RenderingUtils.createProgramFromShaderSrc(m.getRenderOptions().vertexShader,m.getRenderOptions().fragmentShader);
		    vPositionHandler[1+i] = GLES20.glGetAttribLocation(programId[1+i], "vPosition");
		    mvpHandler[1+i] = GLES20.glGetUniformLocation(programId[1+i], "u_MVP");
		    texColorHandler[i] = GLES20.glGetUniformLocation(programId[1+i], "color");
	    }
	    // For the Lemmings
	    programId[1+meshesToRender.size()] = RenderingUtils.createProgramFromShaderSrc(RenderOptions.standardVss,RenderOptions.standardFss);
	    vPositionHandler[1+meshesToRender.size()] = GLES20.glGetAttribLocation(programId[1+meshesToRender.size()], "vPosition");
	    mvpHandler[1+meshesToRender.size()] = GLES20.glGetUniformLocation(programId[1+meshesToRender.size()], "u_MVP");
	    texColorHandler[meshesToRender.size()] = GLES20.glGetUniformLocation(programId[meshesToRender.size()], "color");
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Renderhandlers init done...");
	}

	private SurfaceTexture st2;
	
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
		st.setDefaultBufferSize(1920, 1080);
		st2 = new SurfaceTexture(tex[0]);
		st2.setDefaultBufferSize(1920, 1080);
		
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
