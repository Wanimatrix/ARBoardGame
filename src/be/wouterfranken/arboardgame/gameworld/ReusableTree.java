package be.wouterfranken.arboardgame.gameworld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.wouterfranken.arboardgame.app.AppConfig;
import be.wouterfranken.arboardgame.gameworld.Pathfinder.Node;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;

public class ReusableTree {
	private Map<Pathfinder.Node, Pair<Integer,Node>> perState = new HashMap<Pathfinder.Node, Pair<Integer,Node>>();
	private SparseArray<Pair<float[], Set<Integer>>> perPath = new SparseArray<Pair<float[],Set<Integer>>>();
	private Map<Pathfinder.Node, Integer> generated = new HashMap<Pathfinder.Node, Integer>();
	private int searchCount = -1;
	
	public Integer getId(Node n) {
		return perState.get(n).first;
	}
	
	public void setId(Node n, int newId) {
		Pair<Integer,Node> stateVars = perState.get(n);
		if(stateVars == null)
			perState.put(n,new Pair<Integer, Node>(newId,null));
		else
			perState.put(n,new Pair<Integer, Node>(newId,stateVars.second));
	}
	
	public Node getParent(Node n) {
		return perState.get(n).second;
	}
	
	public void setParent(Node n, Node newParent) {
		Pair<Integer,Node> stateVars = perState.get(n);
		if(stateVars == null)
			perState.put(n,new Pair<Integer, Node>(0,newParent));
		else 
			perState.put(n,new Pair<Integer, Node>(stateVars.first,newParent));
	}
	
	public int getGenerated(Node state) {
		Integer gen = generated.get(state);
		if(gen == null) {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Generated: 0");
			return 0;
		}
		else {
			if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "Generated: "+gen);
			return gen;
		}
	}
	
	public void setGenerated(Pathfinder.Node n, int generated) {
		this.generated.put(n, generated);
	}
	
	public float getHmax(Integer pathId) {
		return perPath.get(pathId).first[1];
	}
	
	public void setHmax(Integer pathId, float newHmax) {
		Pair<float[], Set<Integer>> pathVars = perPath.get(pathId);
		if(pathVars == null)
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{-1,newHmax},new HashSet<Integer>()));
		else
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{pathVars.first[0],newHmax},pathVars.second));
	}
	
	public float getHmin(Integer pathId) {
		return perPath.get(pathId).first[0];
	}
	
	public void setHmin(Integer pathId, float newHmin) {
		Pair<float[], Set<Integer>> pathVars = perPath.get(pathId);
		if(pathVars == null)
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{newHmin,-1},new HashSet<Integer>()));
		else
			perPath.put(pathId,new Pair<float[], Set<Integer>>(new float[]{newHmin,pathVars.first[1]},pathVars.second));
	}
	
	public Set<Integer> getPaths(Integer pathId) {
		return perPath.get(pathId).second;
	}
	
	public void initSearchCount() {
		searchCount = 1;
	}
	
	public void incSearchCount() {
		searchCount++;
	}
	
	private int getSearchCount() {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "SearchCount: "+searchCount);
		return searchCount;
	}
	
	private void addNode(Pathfinder.Node n) {
		this.perState.put(n, new Pair<Integer, Node>(0, null));
		setGenerated(n, searchCount);
	}
	
	public void initState(Node n, WorldCoordinate goal) {
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "NodeCoord: "+n.getCoordinate());
		if(this.getGenerated(n) == 0) {
			n.setGScore(Float.POSITIVE_INFINITY);
			n.setHScore(PathUtilities.h(n.getCoordinate(),goal));
		} else if(this.getGenerated(n) != this.getSearchCount()) {
			n.setGScore(Float.POSITIVE_INFINITY);
		}
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "SearchCount: "+searchCount);
		setGenerated(n, searchCount);
		if(AppConfig.DEBUG_LOGGING) Log.d("PATHFINDER", "New generated: "+this.getGenerated(n));
	}
	
	public void addPath(Node s, WorldCoordinate start, WorldCoordinate goal) {
		if(s.getCoordinate() != goal) {
			getPaths(getId(s)).add(searchCount);
		}
		perPath.put(searchCount, new Pair<float[], Set<Integer>>(new float[]{PathUtilities.h(s.getCoordinate(),goal),PathUtilities.h(start,goal)}, new HashSet<Integer>()));
		while(!s.getCoordinate().equals(start)) {
			Node sTmp = s;
			s = s.getPreviousNode();
//			Log.d("PATHFINDER", "Current Node: "+s.getCoordinate());
			setId(s, searchCount);
			setParent(s, sTmp);
		}
	}
	
	public void removePaths(Node s, WorldCoordinate start, WorldCoordinate goal) {
		int x = getId(s);
		if(getHmax(x) > PathUtilities.h(perState.get(s).second.getCoordinate(),goal))
			setHmax(x, PathUtilities.h(perState.get(s).second.getCoordinate(),goal));
		List<Integer> q = new ArrayList<Integer>();
		for (Integer x2 : getPaths(x)) {
			if(getHmax(x) < getHmax(x2)) {
				q.add(x2);
				getPaths(x).remove(x2);
			}
		}
		
		while(!q.isEmpty()) {
			Integer first = q.get(0);
			if(getHmax(first) > getHmin(first)) {
				setHmax(first, getHmin(first));
				for (Integer first2 : getPaths(first)) {
					q.add(first2);
					getPaths(x).remove(first2);
				}
			}
		}
	}
}
