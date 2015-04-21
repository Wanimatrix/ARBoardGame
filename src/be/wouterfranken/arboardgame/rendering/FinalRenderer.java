package be.wouterfranken.arboardgame.rendering;

import java.nio.Buffer;

import javax.microedition.khronos.egl.EGLConfig;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.FullSquadMesh;
import be.wouterfranken.arboardgame.utilities.RenderingUtils;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.CardboardView.StereoRenderer;
import com.google.vrtoolkit.cardboard.EyeTransform;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

public class FinalRenderer implements StereoRenderer {
	
	private static final String TAG = FinalRenderer.class.getSimpleName();
	
	private Buffer pVertex;
	private Buffer pTexCoord;
	
	// SHADERS
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
		"precision mediump float;\n" +
		"uniform sampler2D sTexture;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  gl_FragColor = texture2D(sTexture,texCoord);\n" +
		"}";
	
	// OPENGL MATRICES
	private float[] mv = new float[16];
    private float[] mvp = new float[16];
	
    // RENDERING HANDLERS
    private int programId;
	private int vPositionHandler;
	private int vTexCoordHandler;
	private int sTextureHandler;
	private int mvpHandler;
	private int texHandler;
	
	private GLSurfaceView view;
	
	public FinalRenderer(GLSurfaceView view) {
		if(AppConfig.VR_MODE) {
		    Matrix.setIdentityM(mv, 0);
		    Matrix.translateM(mv, 0, 0, 0, -3.4f);
		    Matrix.scaleM(mv, 0, 1f, AppConfig.ASPECT_RATIO, 1f);
		} else {
			Matrix.setIdentityM(mv, 0);
			Matrix.translateM(mv, 0, 0, 0, -2.0f);
		    Matrix.scaleM(mv, 0, 1f, 1920/1600.0f, 1f);
		}
	    
	    this.view = view;
	}

	/**
	 ***********
	 * SETTERS *
	 ***********
	 */
	
	public void setTexHandler(int texHandler) {
		this.texHandler = texHandler;
	}

	/**
	 **************************
	 * SURFACE INIT FUNCTIONS *
	 **************************
	 */
	
	@Override
	public void onSurfaceCreated(EGLConfig arg0) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting renderhandlers init...");
		
		programId = RenderingUtils.createProgramFromShaderSrc(vss,fss);
		
		vPositionHandler = GLES20.glGetAttribLocation(programId, "vPosition");
	    vTexCoordHandler = GLES20.glGetAttribLocation ( programId, "vTexCoord" );
	    sTextureHandler = GLES20.glGetUniformLocation ( programId, "sTexture" );
	    mvpHandler = GLES20.glGetUniformLocation(programId, "u_MVP");
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Renderhandlers init done...");
	}

	@Override
	public void onSurfaceChanged(int width, int height) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting texture and vertices setup...");
		
		GLES20.glViewport(0, 0, width, height);
		
		FullSquadMesh squad = new FullSquadMesh();
		
		pVertex = squad.getVertices();
		pTexCoord = squad.getTexCoords();
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Texture and vertices setup done...");
	}

	/**
	 ******************************
	 * RENDERER CONTROL FUNCTIONS *
	 ******************************
	 */
	
	@Override
	public void onNewFrame(HeadTransform arg0) {
		
	 	GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, ((CardboardView)view).getDistortionCorrectionEnabled() ? 2 : 0);
	 	GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
	 	
	 	RenderingUtils.checkGLError("Problem on line "+new Throwable().getStackTrace()[0].getLineNumber());
	 	GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GLES20.glUseProgram(programId);
		RenderingUtils.checkGLError("Problem on line "+new Throwable().getStackTrace()[0].getLineNumber());
	}

	@Override
	public void onDrawEye(EyeTransform transform) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting stereorendering...");
	    
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texHandler);
		GLES20.glUniform1i(sTextureHandler, 1);
	    
	    GLES20.glVertexAttribPointer(vPositionHandler, 2, GLES20.GL_FLOAT, false, 4*2, pVertex);
	    GLES20.glEnableVertexAttribArray(vPositionHandler);
	    GLES20.glVertexAttribPointer(vTexCoordHandler, 2, GLES20.GL_FLOAT, false, 4*2, pTexCoord );
	    GLES20.glEnableVertexAttribArray(vTexCoordHandler);
	    
        Matrix.multiplyMM(mvp, 0, transform.getPerspective(), 0,
                mv, 0);
        
	    GLES20.glUniformMatrix4fv(mvpHandler, 1, false, mvp, 0);
	  
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	    GLES20.glFlush();
	    
	    RenderingUtils.checkGLError("Problem on line "+new Throwable().getStackTrace()[0].getLineNumber());
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Stereorendering done...");
	}
	
	@Override
	public void onFinishFrame(Viewport arg0) {
//		view.requestRender();
	}

	@Override
	public void onRendererShutdown() {
	}
}
