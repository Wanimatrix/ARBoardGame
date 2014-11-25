package be.wouterfranken.arboardgame.utilities;

public class MathUtilities {
	public static float distance(float x0, float y0, float x1, float y1) {
		return (float) Math.sqrt((x1 - x0)*(x1 - x0) + (y1 - y0)*(y1 - y0));
	}
}
