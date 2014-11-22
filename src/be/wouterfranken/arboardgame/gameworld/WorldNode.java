package be.wouterfranken.arboardgame.gameworld;


public class WorldNode {
	private WorldCoordinate left = null;
	private WorldCoordinate right = null;
	private WorldCoordinate top = null;
	private WorldCoordinate bottom = null;
	
	public WorldCoordinate getBottom() {
		return bottom;
	}
	
	public WorldCoordinate getLeft() {
		return left;
	}
	
	public WorldCoordinate getRight() {
		return right;
	}
	
	public WorldCoordinate getTop() {
		return top;
	}
	
	public void setBottom(WorldCoordinate bottom) {
		this.bottom = bottom;
	}
	
	public void setLeft(WorldCoordinate left) {
		this.left = left;
	}
	
	public void setRight(WorldCoordinate right) {
		this.right = right;
	}
	
	public void setTop(WorldCoordinate top) {
		this.top = top;
	}
}
