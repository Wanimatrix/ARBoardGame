package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.List;

import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.tracking.Tracker;
import be.wouterfranken.arboardgame.utilities.MathUtilities;

public class LemmingsGenerator extends Tracker{
	private static final String TAG = LemmingsGenerator.class.getSimpleName();
	
	private List<Lemming> lemmings = new ArrayList<Lemming>();
	private List<Lemming> lemmingsExtern = new ArrayList<Lemming>();
	private Object lock = new Object();
	private Object amountLock = new Object();
	private Object lockExtern = new Object();
	private Object brickLock = new Object();
	private int amount;
	private WorldCoordinate start;
	private WorldCoordinate end;
	
	private World w;
	
	public LemmingsGenerator() {
		super();
		this.amount = WorldConfig.LEMMINGS_AMOUNT;
		this.start = WorldConfig.STARTPOINT;
		this.end = WorldConfig.ENDPOINT;
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
		synchronized (brickLock) {
			while(w.removeBrick());
			for (int i = 0; i < bricks.length; i++) {
				w.addBrick(bricks[i]);
			}
		}
		
		
		// Generate Lemmings
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
	}
	
	private void generateNewLemming() {
		synchronized (amountLock) {
			if(amount == 0) return;
		}
			List<WorldCoordinate> path = Pathfinder.findPath(start, end, w);
			if(path == null) throw new IllegalStateException("You cannot place a LegoBrick on top of the startPosition!");
		synchronized (lock) {
			synchronized (amountLock) {
				if(amount == 0) return;
				lemmings.add(new Lemming(WorldConfig.LEMMINGS_SIZE, WorldConfig.LEMMING_HEIGHT, start.x, start.y, WorldConfig.LEMMINGS_SPEED, path, WorldConfig.LEMMINGS_COLOR));
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
}
