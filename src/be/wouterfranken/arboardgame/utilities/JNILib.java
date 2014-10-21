package be.wouterfranken.arboardgame.utilities;

public class JNILib {
	
//	public static native void saveMat(String matName, long matPtr, String path);
	
//	public static native void storeTrackerKeypoints(long imgMatPtr, String keypointPath, String descPath);
	
//	public static native void loadMatFromFile(String filePath, String matName, long matPtr);
	
//	public static native void getCameraPose(String path_object, long sceneImgPtr, String objKeypointsPath, String objDescriptorsPath, long cameraPosePtr);
	
	public static native void saveMat(String filePath, String matName, long matPtr);

	public static native void loadMatFromFile(String filePath, String matName, long matPtr);
}
