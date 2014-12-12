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
	
	public final static Map<ColorName, Color> COLOR_MAP;
	static {
		COLOR_MAP = new HashMap<ColorName, Color>();
		COLOR_MAP.put(ColorName.RED, new Color(1, 0, 0, 1));
		COLOR_MAP.put(ColorName.YELLOW, new Color(1, 1, 0, 1));
		COLOR_MAP.put(ColorName.BLUE, new Color(0, 0, 1, 1));
	}
	
	
	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}
}
