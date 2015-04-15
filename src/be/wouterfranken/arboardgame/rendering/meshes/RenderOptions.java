package be.wouterfranken.arboardgame.rendering.meshes;

import android.opengl.Matrix;
import be.wouterfranken.arboardgame.utilities.Color;


public class RenderOptions {
	public static final String standardVss =
		"#version 100\n" +
		"attribute vec3 vPosition;\n" +
		"uniform mat4 u_MVP;\n" +
		"void main() {\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, vPosition.z, 1.0 );\n" +
		"}";
	 
	public static final String standardFss =
		"#version 100\n" +
		"precision mediump float;\n" +
		"uniform vec4 color;\n" +
		"void main() {\n" +
		"  gl_FragColor = color;\n" +
		"}";
	
	public static final float[] noTransformation;
	static {
		noTransformation = new float[16];
		Matrix.setIdentityM(noTransformation, 0);
	}
	
	public final boolean useMVP;
	public final Color col;
	public final String vertexShader;
	public final String fragmentShader;
	public final boolean castShadow;
	public final float[] transformation;
	
//	public RenderOptions(boolean useMVP, Color color) {
//		this(useMVP,color, null, standardVss, standardFss);
//	}
	
	public RenderOptions(boolean useMVP, Color color, boolean castShadow) {
		this(useMVP,color, castShadow, standardVss, standardFss);
	}
	
	public RenderOptions(boolean useMVP, Color color, boolean castShadow, float[] transformation) {
		this(useMVP,color, castShadow, standardVss, standardFss, transformation);
	}
	
	public RenderOptions(boolean useMVP, Color color, boolean castShadow, String vertexShader, String fragmentShader) {
		this(useMVP,color, castShadow, standardVss, standardFss, noTransformation);
	}
	
	public RenderOptions(boolean useMVP, Color color, boolean castShadow, String vertexShader, String fragmentShader, float[] transformation) {
		this.useMVP = useMVP;
		this.col = color;
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.castShadow = castShadow;
		this.transformation = transformation;
	}
}
