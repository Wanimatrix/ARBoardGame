package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;
import be.wouterfranken.arboardgame.rendering.tracking.LegoBrickTracker;
import be.wouterfranken.arboardgame.rendering.tracking.Tracker;
import be.wouterfranken.arboardgame.utilities.MathUtilities;
import be.wouterfranken.experiments.TimerManager;

public class LemmingsGenerator extends Tracker{
	private static final String TAG = LemmingsGenerator.class.getSimpleName();
	
	private List<Lemming> lemmings = new ArrayList<Lemming>();
	private List<Lemming> lemmingsExtern = new ArrayList<Lemming>();
	private Object lock = new Object();
	private Object amountLock = new Object();
	private Object lockExtern = new Object();
	private Object brickLock = new Object();
	private Object starLock = new Object();
	private int amount;
	private WorldCoordinate start;
	private WorldCoordinate end;
	
	private World2 w;
	
	public LemmingsGenerator(LegoBrickTracker brickTracker, CameraPoseTracker cameraPose) {
		super();
		this.amount = WorldConfig.LEMMINGS_AMOUNT;
		this.start = WorldConfig.STARTPOINT;
		this.end = WorldConfig.ENDPOINT;
		this.w = new World2(brickTracker, cameraPose);
	}
	
	@SuppressWarnings("unused")
	public void frameTick() {
		if(!w.isWorldGenerated() || w.getBrickThreshold() == null || w.getBrickThreshold().empty()) return;
		
		synchronized (lock) {
			synchronized (lockExtern) {
				lemmingsExtern.clear();
				for (Lemming l : lemmings) {
					lemmingsExtern.add(l);
				}
			}
		}
		
		// Generate Lemmings
		synchronized (lock) {
			TimerManager.start("", "lemmingUpdate", "");
			boolean noLemmings = lemmings.isEmpty();
			if(noLemmings && amount != 0) {
				generateNewLemming();
			} else if (!noLemmings){
				for(int i = 0; i< lemmings.size();i++) {
					Lemming lemming = lemmings.get(i);
					if(lemming.getLocationX() == end.x && lemming.getLocationY() == end.y) {
						lemmings.remove(i--);
						if(WorldConfig.ONE_PER_ONE)
							generateNewLemming();
					} else {
						lemming.updateLocation(end, w);
					}
				}
				if(!WorldConfig.ONE_PER_ONE && !lemmings.isEmpty()){
					Lemming lastNew;
					lastNew = lemmings.get(lemmings.size()-1);
					if(MathUtilities.distance(lastNew.getLocationX(), lastNew.getLocationY(), start.x, start.y) >= WorldConfig.LEMMING_DISTANCE) {
						generateNewLemming();
					}
				}
			}
			TimerManager.stop();
		}
	}
	
	private void generateNewLemming() {
		synchronized (amountLock) {
			if(amount == 0) return;
		}
		Lemming l;
		if(AppConfig.TREE_ADAPTIVE_ASTAR) {
			l = new Lemming(WorldConfig.LEMMINGS_SIZE, WorldConfig.LEMMING_HEIGHT, start.x, start.y, WorldConfig.LEMMINGS_SPEED_WITH_STARS, WorldConfig.LEMMINGS_COLOR);
			l.generatePath(start, end, w);
		} else {
			LemmingPath path = PathFinderOrig.findPath(start, end, w);
			if(path == null) return;//throw new IllegalStateException("You cannot place a LegoBrick on top of the startPosition!");
			l = new Lemming(WorldConfig.LEMMINGS_SIZE, WorldConfig.LEMMING_HEIGHT, start.x, start.y, path, WorldConfig.LEMMINGS_SPEED_WITH_STARS, WorldConfig.LEMMINGS_COLOR);
		}
		synchronized (lock) {
			synchronized (amountLock) {
				if(amount == 0) return;
				if(WorldConfig.ENABLE_STARS) w.addStars();
				lemmings.add(l);
				amount--;
			}
		}
	}
	
	public List<MeshObject> getLemmingMeshes() {
		
		List<MeshObject> lemmingMeshes = new ArrayList<MeshObject>();
		if(!w.isWorldGenerated()) return lemmingMeshes;
		synchronized (lockExtern) {
			for (Lemming lemming : lemmingsExtern) {
				lemmingMeshes.add(lemming.getMesh());
			}
		}
		return lemmingMeshes;
	}
	
	public List<MeshObject> getStarMeshes() {
		List<MeshObject> starMeshes = new ArrayList<MeshObject>();
		if(!w.isWorldGenerated()) return starMeshes;
		for (MeshObject starMesh : w.getStarMeshes()) {
			starMeshes.add(starMesh);
		}
		return starMeshes;
	}
	
//	public List<MeshObject> getActiveBrickMeshes(RenderOptions ro) {
//		List<MeshObject> brickMeshes = new ArrayList<MeshObject>();
//		if(!w.isWorldGenerated()) return brickMeshes;
//		for (LegoBrick brick : w.getActiveBricks()) {
//			brickMeshes.add(brick.getMesh(ro));
//		}
//		return brickMeshes;
//	}
}
