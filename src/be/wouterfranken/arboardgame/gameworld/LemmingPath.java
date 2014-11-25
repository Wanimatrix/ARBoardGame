package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

public class LemmingPath {
	private List<WorldCoordinate> coordinateList;
	
	public LemmingPath(List<WorldCoordinate> coordinateList) {
		this.coordinateList = coordinateList;
	}
	
	public WorldCoordinate getPathElement(int i) {
		return coordinateList.get(i);
	}
	
	public void removePathElement(int i) {
		coordinateList.remove(i);
	}
}
