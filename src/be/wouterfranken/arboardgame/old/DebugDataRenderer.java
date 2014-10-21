package be.wouterfranken.arboardgame.old;
//package be.wouterfranken.arboardgame;
//
//import be.wouterfranken.arboardgame.utility.RenderingUtils;
//import android.opengl.GLES20;
//import android.opengl.Matrix;
//
//public class DebugDataRenderer {
//	
//	private final static String TAG = DebugDataRenderer.class.getSimpleName();
//	
//	private static DebugDataRenderer instance = new DebugDataRenderer();
//	
//	private int[] shaderProgramIds;
//	private int[] vertexHandles;
//	private int[] mvpHandles;
//	private int[] opHandles;
//	private int[] colorHandles;
//	private int[] kHandles;
//	private int[] vTexCoordHandles;
//	private int[] screenDimenHandles;
//	private String[] vShaders;
//	private String[] fShaders;
//	
//	
//	// Shader for vertices of GBO
//	public static final String GBO_VERTEX_SHADER =  			
//		"attribute vec3 vPosition;\n" +
//		"attribute vec2 vTexCoord;\n" +
//	    "uniform mat4 u_MVP;\n" +
//		"varying vec2 texCoord;\n" +
//	    "void main()\n" +
//	    "{\n" +
//	    "    gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, vPosition.z, 1.0 );\n" +
//	    "	 texCoord = vTexCoord;\n" +
//	    "}\n";
//		
//	// Fragment shader of GBO
//	public static final String GBO_FRAGMENT_SHADER_NODISTORTION = 
//		"precision mediump float;\n" +
//		"uniform float opacity;\n" +
//	    "uniform vec3 color;\n" +
//	    "void main ()\n" +
//	    "{\n" +
//	    "    gl_FragColor = vec4(color.r, color.g, color.b, opacity);\n" +
//	    "}\n";
//	
//	// Fragment shader of VideoBackground with distortion correction
//		public static final String GBO_FRAGMENT_SHADER_DISTORTION =
//			"precision mediump float;\n" +
//			"uniform vec2 k;\n" +
//			"uniform vec4 screenDimen;\n"+
//			"uniform float opacity;\n" +
//		    "uniform vec3 color;\n" +
//			"varying vec2 texCoord;\n" +
//			"void main()\n" +
//			"{\n" +
//			"	vec2 screenCenter = vec2(screenDimen[0] + (screenDimen[1]-screenDimen[0])/2.0, screenDimen[2] + (screenDimen[3]-screenDimen[2])/4.0);  /* Find the screen center */\n" +
//			"	float norm = length(screenCenter);  /* Distance between corner and center */\n" +
//			
//			"	// get a vector from center to where we are at now (in screen space) and normalize it\n" +
//			"	vec2 radial_vector = ( texCoord - screenCenter ) / norm;\n" +
//			"	float radial_vector_len = length(radial_vector);\n" +
//			"	vec2 radial_vector_unit = radial_vector / radial_vector_len;\n" +
//			
//			"	// Compute the new distance from the screen center.\n" +
//			"	vec2 new_dist = vec2(radial_vector_len + k.y * pow(radial_vector_len,3.0),radial_vector_len + k.x * pow(radial_vector_len,3.0));\n" +
//			
//			"	/* Now, compute texture coordinate we want to lookup. */\n" +
//			
//			"	// Find the coordinate we want to lookup\n" +
//			"	vec2 warp_coord = texCoord;\n" +
//			"	if(length(new_dist) >= 0.01) {\n" +
//			"		warp_coord = radial_vector_unit * (new_dist * norm);\n" +
//			
//			"		// Translate the coordinte such that the (0,0) is back at the screen center\n" +
//			"		warp_coord = warp_coord + screenCenter;\n" +
//			
//			"	}\n" +
//			
//			"	/* If we lookup a texture coordinate that is not on the texture, return a solid color */\n" +
//			"	if ((warp_coord.s > float(screenDimen[1])  || warp_coord.s < screenDimen[0]) ||\n" +
//			"	    (warp_coord.t > float(screenDimen[3]) || warp_coord.t < screenDimen[2]))\n" +
//			"		gl_FragColor = vec4(0,0,0,1); // black\n" +
//			"	else\n" +
//			"		gl_FragColor = vec4(color.r, color.g, color.b, opacity);  // lookup into the texture\n" +
//			"}\n";
//	
//	private enum DEBUG_TYPE{
//		GBO(0);
//		
//		private final int value;
//	    private DEBUG_TYPE(int value) {
//	        this.value = value;
//	    }
//		
//		public int getValue() {
//	        return value;
//	    }
//	}
//	
//	public DebugDataRenderer() {
//		shaderProgramIds = new int[DEBUG_TYPE.values().length];
//		vertexHandles = new int[DEBUG_TYPE.values().length];
//		mvpHandles = new int[DEBUG_TYPE.values().length];
//		opHandles = new int[DEBUG_TYPE.values().length];
//		colorHandles = new int[DEBUG_TYPE.values().length];
//		kHandles = new int[DEBUG_TYPE.values().length];
//		screenDimenHandles = new int[DEBUG_TYPE.values().length];
//		vTexCoordHandles = new int[DEBUG_TYPE.values().length];
//		vShaders = new String[] {GBO_VERTEX_SHADER};
//		String[] fShaders_noDistort = new String[] {GBO_FRAGMENT_SHADER_NODISTORTION};
//		String[] fShaders_distort = new String[] {GBO_FRAGMENT_SHADER_DISTORTION};
//		
//		fShaders = fShaders_noDistort;
//		
//		for (DEBUG_TYPE type : DEBUG_TYPE.values()) {
//			shaderProgramIds[type.getValue()] = RenderingUtils.createProgramFromShaderSrc(vShaders[type.getValue()], fShaders[type.getValue()]);
//			vertexHandles[type.getValue()] = GLES20.glGetAttribLocation(shaderProgramIds[type.getValue()], "vPosition");
//			mvpHandles[type.getValue()] = GLES20.glGetUniformLocation(shaderProgramIds[type.getValue()], "u_MVP");
//			opHandles[type.getValue()] = GLES20.glGetUniformLocation(shaderProgramIds[type.getValue()], "opacity");
//			colorHandles[type.getValue()] = GLES20.glGetUniformLocation(shaderProgramIds[type.getValue()], "color");
//			kHandles[type.getValue()] = GLES20.glGetUniformLocation(shaderProgramIds[type.getValue()], "k");
//			screenDimenHandles[type.getValue()] = GLES20.glGetUniformLocation(shaderProgramIds[type.getValue()], "screenDimen");
//			vTexCoordHandles[type.getValue()] = GLES20.glGetAttribLocation(shaderProgramIds[type.getValue()], "vTexCoord");
//		}
//	}
//	
//	public void renderGameBoardOverlay(float[] mvpMatrix, float[] targetSize) {
//		final int GBOidx = DEBUG_TYPE.GBO.getValue();
//		
//		GameBoardOverlay gbo = new GameBoardOverlay(targetSize); 
//		
//		// activate the shader program and bind the vertex/normal/tex coords
//        GLES20.glUseProgram(shaderProgramIds[0]);
//       
//        GLES20.glDisable(GLES20.GL_CULL_FACE);
//        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
//        GLES20.glEnable(GLES20.GL_BLEND);
//        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
//        
//        GLES20.glVertexAttribPointer(vertexHandles[GBOidx], 3, GLES20.GL_FLOAT,
//            false, 0, gbo.getVertices());
//        GLES20.glVertexAttribPointer(vTexCoordHandles[GBOidx], 2, GLES20.GL_FLOAT, false, 0, gbo.getTexCoords());
//        
//        GLES20.glEnableVertexAttribArray(vertexHandles[GBOidx]);
//        
//        GLES20.glUniformMatrix4fv(mvpHandles[GBOidx], 1, false,
//        		mvpMatrix, 0);
//        GLES20.glUniform1f(opHandles[GBOidx], 0.8f);
//        GLES20.glUniform3f(colorHandles[GBOidx], 1.0f, 1.0f, 0);
//        GLES20.glUniform2f(kHandles[GBOidx], 0.15f, 0.05f);
//        GLES20.glUniform4f(screenDimenHandles[GBOidx], 0.125f, 0.625f, 0.0f, 1.0f);
//        
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0,
//        		gbo.getNumObjectVertex());
//        
//        RenderingUtils.checkGLError("DebugRenderer GameBoardOverlay");
//        
//        GLES20.glDisable(GLES20.GL_BLEND);
//	}
//	
//	public static DebugDataRenderer getInstance() {
//		return instance;
//	}
//}
