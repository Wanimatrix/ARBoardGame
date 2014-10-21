package be.wouterfranken.arboardgame.utilities;

import org.opencv.core.Mat;
import org.opencv.core.Point;

import be.wouterfranken.arboardgame.app.AppConfig;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

public class RenderingUtils {
	private static final String TAG = "RenderingUtils";
	
	public static int createProgramFromShaderSrc(String vertexShaderSrc, String fragmentShaderSrc)
    {
        int vertShader = initShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc);
        int fragShader = initShader(GLES20.GL_FRAGMENT_SHADER,
            fragmentShaderSrc);
        
        if (vertShader == 0 || fragShader == 0)
            return 0;
        
        int program = GLES20.glCreateProgram();
        if (program != 0)
        {
            GLES20.glAttachShader(program, vertShader);
            checkGLError("glAttchShader(vert)");
            
            GLES20.glAttachShader(program, fragShader);
            checkGLError("glAttchShader(frag)");
            
            GLES20.glLinkProgram(program);
            int[] glStatusVar = { GLES20.GL_FALSE };
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, glStatusVar,
                0);
            if (glStatusVar[0] == GLES20.GL_FALSE)
            {
                Log.e(
                    TAG,
                    "Could NOT link program : "
                        + GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        
        return program;
    }
	
	private static int initShader(int shaderType, String source)
    {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0)
        {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
            
            int[] glStatusVar = { GLES20.GL_FALSE };
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, glStatusVar,
                0);
            if (glStatusVar[0] == GLES20.GL_FALSE)
            {
                Log.e(TAG, "Could NOT compile shader " + shaderType + " : "
                    + GLES20.glGetShaderInfoLog(shader));
                GLES20.glDeleteShader(shader);
                shader = 0;
            }
            
        }
        
        return shader;
    }
	
	/**
	 * Checks for openGL errors.
	 * @param op
	 */
    public static void checkGLError(String op)
    {
        for (int error = GLES20.glGetError(); error != 0; error = GLES20
            .glGetError())
            Log.e(
                TAG,
                "After operation " + op + " got glError 0x"
                    + Integer.toHexString(error));
    }
    
    
    public static float[] toOpenGLArray(Point[] points) {
//    	if(points.length == 4) {
//    		Log.d(TAG,"Corners: ("
//        			+ points[0].x+","
//        			+ points[0].y+") ("
//        			+ points[1].x+","
//        			+ points[1].y+") ("
//        			+ points[2].x+","
//        			+ points[2].y+") ("
//        			+ points[3].x+","
//        			+ points[3].y+")");
//    	} 
    	
    	float[] result = new float[points.length*2];
    	for (int i = 0; i < result.length; i+=2) {
			result[i] = (float)((float)points[i/2].x/AppConfig.PREVIEW_RESOLUTION[0]);
			result[i+1] = (float)((float)points[i/2].y/AppConfig.PREVIEW_RESOLUTION[1]);
		}
    	return result;
    }
}
