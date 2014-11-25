package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Point;

import be.wouterfranken.arboardgame.rendering.meshes.CubeMesh;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class LemmingsGenerator {
	private List<Lemming> lemmings = new ArrayList<Lemming>();
	private int amount;
	private WorldCoordinate start;
	private WorldCoordinate end;
	
	private World w;
	
	public LemmingsGenerator() {
		this.amount = WorldConfig.LEMMINGS_AMOUNT;
		this.start = WorldConfig.STARTPOINT;
		this.end = WorldConfig.ENDPOINT;
		this.w = new World();
	}
	
	public void frameTick() {
		if(!w.isWorldGenerated()) return;
		if(lemmings.isEmpty() && amount != 0) {
			generateNewLemming();
		} else if (!lemmings.isEmpty()){
			for(int i = 0; i< lemmings.size();i++) {
				Lemming lemming = lemmings.get(i);
				if(lemming.getLocationX() == end.x && lemming.getLocationY() == end.y) {
					lemmings.remove(i--);
				} else {
					lemming.updateLocation();
				}
			}
			
			Lemming lastNew = lemmings.get(lemmings.size()-1);
			if(MathUtilities.distance(lastNew.getLocationX(), lastNew.getLocationY(), start.x, start.y) >= WorldConfig.LEMMING_DISTANCE) {
				generateNewLemming();
			}
		}
	}
	
	private void generateNewLemming() {
		if(amount == 0) return;
		List<WorldCoordinate> path = Pathfinder.findPath(start, end, w);
		Pathfinder.findPath2(start, end, w);
		lemmings.add(new Lemming(WorldConfig.LEMMINGS_SIZE, WorldConfig.LEMMING_HEIGHT, start.x, start.y, WorldConfig.LEMMINGS_SPEED, path, WorldConfig.LEMMINGS_COLOR));
		amount--;
	}
	
	public List<MeshObject> getLemmingMeshes() {
		List<MeshObject> lemmingMeshes = new ArrayList<MeshObject>();
		if(!w.isWorldGenerated()) return lemmingMeshes;
		for (Lemming lemming : lemmings) {
			lemmingMeshes.add(lemming.getMesh());
		}
		return lemmingMeshes;
	}
}
