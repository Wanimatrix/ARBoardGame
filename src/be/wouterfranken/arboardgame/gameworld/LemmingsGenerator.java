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
	
//	private World2 w;
	private World w;
	
	public LemmingsGenerator(LegoBrickTracker brickTracker, CameraPoseTracker cameraPose) {
		super();
		this.amount = WorldConfig.LEMMINGS_AMOUNT;
		this.start = WorldConfig.STARTPOINT;
		this.end = WorldConfig.ENDPOINT;
//		this.w = new World2(brickTracker, cameraPose);
		this.w = new World();
	}
	
	@SuppressWarnings("unused")
	public void frameTick(LegoBrick[] bricks) {
		if(!w.isWorldGenerated()) return;
		
		synchronized (lock) {
			synchronized (lockExtern) {
				lemmingsExtern.clear();
				for (Lemming l : lemmings) {
					lemmingsExtern.add(l);
				}
			}
		}
		
		// Brick control
//		synchronized (brickLock) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "BrickAmount: "+bricks.length);
		Log.d(TAG, "Start adding bricks");
		TimerManager.start("BrickDetection", "addBricks", "/sdcard/arbg/oldTimeAddBricks.txt");
		w.addBricks(bricks);
		TimerManager.stop();
	
		// Generate Lemmings
		TimerManager.start("BrickDetection", "lemmingUpdate", "/sdcard/arbg/oldTimeUpdateLemming.txt");
		synchronized (lock) {
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
		}
		TimerManager.stop();
//		}
	}
	
	private void generateNewLemming() {
		synchronized (amountLock) {
			if(amount == 0) return;
		}
//		Lemming l = 
//				new Lemming(WorldConfig.LEMMINGS_SIZE, WorldConfig.LEMMING_HEIGHT, start.x, start.y, WorldConfig.LEMMINGS_SPEED_WITH_STARS, WorldConfig.LEMMINGS_COLOR);
//		l.generatePath(start, end, w);
		LemmingPath path = new LemmingPath();
		path.addAll(PathFinderOrig1.findPath(start, end, w));
		if(path == null) throw new IllegalStateException("You cannot place a LegoBrick on top of the startPosition!");
		synchronized (lock) {
			synchronized (amountLock) {
				if(amount == 0) return;
				w.addStars();
				lemmings.add(new Lemming(WorldConfig.LEMMINGS_SIZE, WorldConfig.LEMMING_HEIGHT, start.x, start.y, System.nanoTime(), path, WorldConfig.LEMMINGS_SPEED_WITH_STARS, WorldConfig.LEMMINGS_COLOR));
//				lemmings.add(l);
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
	
	public List<MeshObject> getActiveBrickMeshes(RenderOptions ro) {
		List<MeshObject> brickMeshes = new ArrayList<MeshObject>();
		if(!w.isWorldGenerated()) return brickMeshes;
		for (LegoBrick brick : w.getActiveBricks()) {
			brickMeshes.add(brick.getMesh(ro));
		}
		return brickMeshes;
	}
	
	public World getWorld() {
		return w;
	}
}
