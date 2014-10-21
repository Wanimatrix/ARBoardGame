package be.wouterfranken.arboardgame.rendering;

import java.nio.Buffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfPoint3f;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.GameBoardOverlayMesh;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPose;
import be.wouterfranken.arboardgame.utilities.DebugUtilities;
import be.wouterfranken.arboardgame.utilities.RenderingUtils;

import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.EyeParams.Eye;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class VirtualLayerRenderer implements StereoRenderer, PreviewCallback {
	
	private CameraPose cameraPose;
	private final static String TAG = VirtualLayerRenderer.class.getSimpleName();
	private Camera camera;
	private int programId;
	private Buffer vertex;
	
	private float[] mv = new float[16];
    private float[] mvp = new float[16];
    private int vPositionHandler;
    private int mvpHandler;
//    private float[] glCameraPose = null;
    private float[] glProj = null;
    private float[] glMv = null;
    private Context context = null;
    
    private GameBoardOverlayMesh gbo = new GameBoardOverlayMesh(new float[]{0.064f,0.064f});
	
	public VirtualLayerRenderer(CameraPose cameraPose, Camera camera, Context context) {
		this.cameraPose = cameraPose;
		this.camera = camera;
		this.context = context;
		
		Matrix.setIdentityM(mv, 0);
	    Matrix.translateM(mv, 0, 0f, 0f, -3.4f);
	    Matrix.scaleM(mv, 0, 1f, AppConfig.ASPECT_RATIO, 1f);
	}
	
	private final String vss =
		"attribute vec3 vPosition;\n" +
		"uniform mat4 u_MVP;\n" +
		"void main() {\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, 0, 1.0 );\n" +
		"}";
 
	private final String fss =
		"precision mediump float;\n" +
		"void main() {\n" +
		"  gl_FragColor = vec4(1.0,0,0,0.5);\n" +
		"}";
    
    
	
	@Override
	public void onDrawEye(EyeTransform transform) {
		
//		if(cameraPose.getRenderImgSize() == null) {
//        	float[][] vertices = {{-1,-1,0,1},{1,1,0,1}};
//	        float[] tmp = new float[16];
//	        Matrix.multiplyMM(tmp, 0, transform.getPerspective(), 0, mv, 0);
//	        Matrix.transposeM(tmp, 0, tmp, 0);
//	        Matrix.multiplyMV(vertices[0], 0, tmp, 0, vertices[0], 0);
//	        Matrix.multiplyMV(vertices[1], 0, tmp, 0, vertices[1], 0);
//	        DebugUtilities.logGLMatrix("Vertices0: ", vertices[0], 1, 4);
//	        DebugUtilities.logGLMatrix("Vertices1: ", vertices[1], 1, 4);
//	        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//	        Display display = wm.getDefaultDisplay();
//	        Point size = new Point();
//	        display.getSize(size);
//	        
//	        DisplayMetrics metrics = new DisplayMetrics();
//	        display.getMetrics(metrics); 
//
//	        // Convert from dots per inch to dots per centimetre.
//	        float xdpc = metrics.xdpi / 2.54f; 
//	        float ydpc = metrics.ydpi / 2.54f;
//	        cameraPose.setRenderImgSize(new Size(((2*Math.abs(vertices[0][0]))/(size.x/xdpc))*size.x,
//	        						 ((2*Math.abs(vertices[0][1]))/(size.y/ydpc))*size.y));
//        }
		
		if(glProj != null && glMv != null && vertex != null) {
			GLES20.glUseProgram(programId);
			GLES20.glEnable(GLES20.GL_BLEND);
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			
			GLES20.glVertexAttribPointer(vPositionHandler, 3, GLES20.GL_FLOAT, false, 4*3, vertex);
		    GLES20.glEnableVertexAttribArray(vPositionHandler);
		    
		    Matrix.multiplyMM(mvp, 0, glProj, 0, glMv, 0);
		    
//		    float[] pt = new float[]{0.032f,0.032f,0,1};
//		    float[] res = new float[4];
//		    Matrix.multiplyMV(res, 0, mvp, 0, pt, 0);
//		    DebugUtilities.logGLMatrix("Result", res, 1, 4);
		    
		    Matrix.multiplyMM(mvp, 0, mv, 0, mvp, 0);
	        
	        float[] ndc = new float[16];
	        float[] persp = new float[16];
	        float[] project = new float[16];
	        
	        float far = 100;
	        float near = 0.01f;
	        float right = (float)-Math.tan(Math.toRadians(transform.getParams().getFov().getRight())) * near;
	        float left = (float)Math.tan(Math.toRadians(transform.getParams().getFov().getLeft())) * near;
	        float bottom = (float)-Math.tan(Math.toRadians(transform.getParams().getFov().getBottom())) * near;
	        float top = (float)Math.tan(Math.toRadians(transform.getParams().getFov().getTop())) * near;
	        
	        Mat camMatrix = new Mat();
	        cameraPose.getCameraCalibration(camMatrix.getNativeObjAddr());
	        persp[0 + 0*4] = near;
	        persp[1 + 1*4] = near;
	        persp[2 + 2*4] = near + far;
	        persp[2 + 3*4] = near * far;
	        persp[3 + 2*4] = -1;
	        
	        ndc[0 + 0*4] = -2.0f/(right-left);
	        ndc[1 + 1*4] = 2.0f/(top-bottom);
	        ndc[2 + 2*4] = -2.0f/(far-near);
	        ndc[0 + 3*4] = -(right+left)/(right-left);
	        ndc[1 + 3*4] = -(top+bottom)/(top-bottom);
	        ndc[2 + 3*4] = -(far+near)/(far-near);
	        ndc[3 + 3*4] = 1;
	        Matrix.multiplyMM(project, 0, ndc, 0, persp, 0);

//	        DebugUtilities.logGLMatrix("Perspective", persp, 4, 4);
//	        DebugUtilities.logGLMatrix("NDC", ndc, 4, 4);
//	        DebugUtilities.logGLMatrix("Projection", project, 4, 4);
	        
//	        DebugUtilities.logGLMatrix("Cardboard Perspective", transform.getPerspective(), 4, 4);
//	        DebugUtilities.logGLMatrix("Own Cardboard Perspective", project, 4, 4);
	        
	        Matrix.multiplyMM(mvp, 0, project, 0, mvp, 0);
	        
		    GLES20.glUniformMatrix4fv(mvpHandler, 1, false, mvp, 0);
		  
		    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
		    GLES20.glFlush();
		    
		    GLES20.glDisable(GLES20.GL_BLEND);
		}
	}

	private long renderDataTimer;
	@Override
	public void onFinishFrame(Viewport vp) {
		Log.d(TAG, "Rendering done in "+(System.currentTimeMillis()-renderDataTimer)+"ms");
	}

	@Override
	public void onNewFrame(HeadTransform ht) {
		renderDataTimer = System.currentTimeMillis();
		double[] mv = cameraPose.getMv();
		double[] proj = cameraPose.getProj();
		if(mv != null && proj != null) {
			glMv = new float[16];
			glProj = new float[16];
			
			for (int i = 0; i < mv.length; i++) {
				glProj[i] = (float) proj[i];
				glMv[i] = (float) mv[i];
			}
			vertex = gbo.getVertices();
		} else {
			glProj = null;
			glMv = null;
			vertex = null;
		} 	
	}

	@Override
	public void onRendererShutdown() {
		
	}

	@Override
	public void onSurfaceChanged(int width, int height) {
		
	}

	@Override
	public void onSurfaceCreated(EGLConfig config) {
		programId = RenderingUtils.createProgramFromShaderSrc(vss, fss);
		
		vPositionHandler = GLES20.glGetAttribLocation(programId, "vPosition");
	    mvpHandler = GLES20.glGetUniformLocation(programId, "u_MVP");
	}

	@Override
	public void onPreviewFrame(byte[] frameData, Camera camera) {
		cameraPose.updateCameraPose(frameData, camera);
	}

}
