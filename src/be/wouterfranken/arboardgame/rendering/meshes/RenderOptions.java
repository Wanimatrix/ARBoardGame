package be.wouterfranken.arboardgame.rendering.meshes;

import be.wouterfranken.arboardgame.utilities.Color;


public class RenderOptions {
	public static final String standardVss =
		"attribute vec3 vPosition;\n" +
		"uniform mat4 u_MVP;\n" +
		"void main() {\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, vPosition.z, 1.0 );\n" +
		"}";
	 
	public static final String standardFss =
		"precision mediump float;\n" +
		"uniform vec4 color;\n" +
		"void main() {\n" +
		"  gl_FragColor = color;\n" +
		"}";
	
	public final boolean useMVP;
	public final Color col;
	public final String vertexShader;
	public final String fragmentShader;
	public final boolean lightPosition;
	
//	public RenderOptions(boolean useMVP, Color color) {
//		this(useMVP,color, null, standardVss, standardFss);
//	}
	
	public RenderOptions(boolean useMVP, Color color, boolean lightPosition) {
		this(useMVP,color, lightPosition, standardVss, standardFss);
	}
	
	public RenderOptions(boolean useMVP, Color color, boolean lightPosition, String vertexShader, String fragmentShader) {
		this.useMVP = useMVP;
		this.col = color;
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
		this.lightPosition = lightPosition;
	}
}
