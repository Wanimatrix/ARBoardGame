package be.wouterfranken.arboardgame.old;
//package be.wouterfranken.arboardgame.sensordata;
//
//public abstract class SensorDataType {
//	protected float[] data;
//	protected boolean dbg = true;
//	
//	public SensorDataType() {
//		data = new float[]{0,0,0};
//	}
//	
//	public void setData(float[] data) {
//		this.data = data;
//	}
//	
//	public boolean isSet(){
//		return !data.equals(new float[]{0,0,0});
//	}
//	
//	public float[] getData() {
//		return data;
//	}
//	
//	public float getData(int axisInt) {
//		return data[axisInt];
//	}
//	
//	public boolean debugEnabled() {
//		return dbg;
//	}
//	
//	@Override
//	public String toString() {
//		return "X "+this.getClass().getSimpleName()+": "+this.data[0]+"\n"
//			  +"Y "+this.getClass().getSimpleName()+": "+this.data[1]+"\n"
//			  +"Z "+this.getClass().getSimpleName()+": "+this.data[2]+"\n";
//	}
//}
