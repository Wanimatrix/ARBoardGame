package be.wouterfranken.arboardgame.utilities;

import java.util.HashMap;
import java.util.Map;

public class Color {
	public final float r;
	public final float g;
	public final float b;
	public final float a;
	
	public enum ColorName {
		RED,
		YELLOW,
		BLUE
	}
	
	public final static Map<ColorName, int[]> COLOR_MAP;
	static {
		COLOR_MAP = new HashMap<ColorName, int[]>();
		COLOR_MAP.put(ColorName.RED, new int[]{1, 0, 0, 1});
		COLOR_MAP.put(ColorName.YELLOW, new int[]{1, 1, 0, 1});
		COLOR_MAP.put(ColorName.BLUE, new int[]{0, 0, 1, 1});
	}
	
	
	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
	
	public Color(ColorName name) {
		int[] theColor = COLOR_MAP.get(name);
		this.r = theColor[0];
		this.g = theColor[1];
		this.b = theColor[2];
		this.a = theColor[3];
	}
	
	@Override
	public String toString() {
		return r+", "+g+", "+b+", "+a;
	}
}
