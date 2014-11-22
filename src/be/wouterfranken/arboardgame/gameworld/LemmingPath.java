package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

public class LemmingPath {
	private List<WorldCoordinate> path = new ArrayList<WorldCoordinate>();
	
	public LemmingPath() {
	}
	
	public void addPiece(WorldCoordinate w) {
		path.add(w);
	} 
}
