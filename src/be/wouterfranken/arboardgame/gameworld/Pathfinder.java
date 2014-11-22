package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.util.Pair;

public class Pathfinder {
	private static final String TAG = Pathfinder.class.getSimpleName();
	
	public static List<WorldCoordinate> findPath(WorldCoordinate start, WorldCoordinate target, World w) {
		Map<WorldCoordinate,Pair<WorldCoordinate,Integer>> queue = new LinkedHashMap<WorldCoordinate,Pair<WorldCoordinate,Integer>>();
		
		queue.put(target,new Pair<WorldCoordinate, Integer>(null, 0));
		WorldCoordinate latestAdded = null;
		Iterator<Map.Entry<WorldCoordinate,Pair<WorldCoordinate,Integer>>> i = queue.entrySet().iterator();
		while (i.hasNext()) { // TODO: Fix this loop: iterator ends after one element, however during the loop elements are succesfully added to the list!
			Map.Entry<WorldCoordinate,Pair<WorldCoordinate,Integer>> entry = i.next();
			Map<WorldCoordinate,Pair<WorldCoordinate,Integer>> neighbours = new HashMap<WorldCoordinate,Pair<WorldCoordinate,Integer>>();
			WorldCoordinate left = w.getNode(entry.getKey()).getLeft();
			WorldCoordinate right = w.getNode(entry.getKey()).getRight();
			WorldCoordinate top = w.getNode(entry.getKey()).getTop();
			WorldCoordinate bottom = w.getNode(entry.getKey()).getBottom();
			if(left!=null){ 
				neighbours.put(left, new Pair<WorldCoordinate, Integer>(entry.getKey(), entry.getValue().second+1));
			}
			if(right!=null) {
				neighbours.put(right,new Pair<WorldCoordinate, Integer>(entry.getKey(), entry.getValue().second+1));
			}
			if(top!=null) {
				neighbours.put(top,new Pair<WorldCoordinate, Integer>(entry.getKey(), entry.getValue().second+1));
			}
			if(bottom!=null) {
				neighbours.put(bottom,new Pair<WorldCoordinate, Integer>(entry.getKey(), entry.getValue().second+1));
			}
			
			
			
			Iterator<Map.Entry<WorldCoordinate,Pair<WorldCoordinate,Integer>>> i2 = neighbours.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry<WorldCoordinate,Pair<WorldCoordinate,Integer>> wco = i2.next();
				if(queue.containsKey(wco.getKey()) && queue.get(wco.getKey()).second >= wco.getValue().second) {
					i2.remove();
				} else {
					queue.put(wco.getKey(), wco.getValue());
//					Log.d(TAG, "New queue size: "+queue.size()+", new entryset size: "+queue.entrySet().size());
					latestAdded = wco.getKey();
					if(wco.getKey().equals(start)) break;
				}
			}
			if(latestAdded != null && latestAdded.equals(start)) {
//				Log.d(TAG, "END: Jumping out of loop at break");
				break;
			}
		}
		
		List<WorldCoordinate> path = new ArrayList<WorldCoordinate>();
		WorldCoordinate current = start;
		while(!current.equals(target)) {
			path.add(current);
			current = queue.get(current).first;
		}
		path.add(target);
		
		return path;
	}
}
