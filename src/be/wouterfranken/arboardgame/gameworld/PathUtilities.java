package be.wouterfranken.arboardgame.gameworld;

public class PathUtilities {
	public static float h(WorldCoordinate s, WorldCoordinate goal) {
		return Math.abs(s.x-goal.x)+Math.abs(s.y-goal.y);
	}
}
