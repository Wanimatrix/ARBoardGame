package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.util.Pair;
import be.wouterfranken.arboardgame.app.AppConfig;

public class Pathfinder {
	private static final String TAG = Pathfinder.class.getSimpleName();
	
	// TODO: adapt to A*
	public static List<WorldCoordinate> findPath(final WorldCoordinate start, WorldCoordinate target, World w) {
		long startTime = System.nanoTime();
		
		Map<WorldCoordinate,Pair<WorldCoordinate,Integer>> map = new LinkedHashMap<WorldCoordinate,Pair<WorldCoordinate,Integer>>();
		List<WorldCoordinate> queue = new ArrayList<WorldCoordinate>();
		
		map.put(target,new Pair<WorldCoordinate, Integer>(null, 0));
		queue.add(target);
		while(true) { 
			WorldCoordinate current = queue.get(0);
			Map<WorldCoordinate,Pair<WorldCoordinate,Integer>> neighbours = new HashMap<WorldCoordinate,Pair<WorldCoordinate,Integer>>();
			WorldCoordinate left = w.getNode(current).getLeft();
			WorldCoordinate right = w.getNode(current).getRight();
			WorldCoordinate top = w.getNode(current).getTop();
			WorldCoordinate bottom = w.getNode(current).getBottom();
			if(left!=null){ 
				neighbours.put(left, new Pair<WorldCoordinate, Integer>(current, map.get(current).second+1));
			}
			if(right!=null) {
				neighbours.put(right,new Pair<WorldCoordinate, Integer>(current, map.get(current).second+1));
			}
			if(top!=null) {
				neighbours.put(top,new Pair<WorldCoordinate, Integer>(current, map.get(current).second+1));
			}
			if(bottom!=null) {
				neighbours.put(bottom,new Pair<WorldCoordinate, Integer>(current, map.get(current).second+1));
			}
			
			Iterator<Map.Entry<WorldCoordinate,Pair<WorldCoordinate,Integer>>> i2 = neighbours.entrySet().iterator();
			while (i2.hasNext()) {
				Map.Entry<WorldCoordinate,Pair<WorldCoordinate,Integer>> wco = i2.next();
				if(map.containsKey(wco.getKey()) && map.get(wco.getKey()).second <= wco.getValue().second) {
					i2.remove();
				} else {
					map.put(wco.getKey(), wco.getValue());
					queue.add(wco.getKey());
					if(wco.getKey().equals(start)) break;
				}
			}
			if(neighbours.containsKey(start)) {
				break;
			}
			queue.remove(0);
			Collections.sort(queue, new Comparator<WorldCoordinate>() {
				@Override
				public int compare(WorldCoordinate lhs,
						WorldCoordinate rhs) {
					if(lhs.distance(start) < rhs.distance(start))
						return -1;
					else if (lhs.distance(start) == rhs.distance(start))
						return 0;
					else return 1;
				}
			});
			
		}
		List<WorldCoordinate> path = new ArrayList<WorldCoordinate>();
		WorldCoordinate current = start;
		while(!current.equals(target)) {
			path.add(current);
			current = map.get(current).first;
		}
		path.add(target);
		
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming path found in "+(System.nanoTime()-startTime)/1000000L+"ms");
		
		return path;
	}
}
