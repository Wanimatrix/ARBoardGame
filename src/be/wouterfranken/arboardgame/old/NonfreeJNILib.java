package be.wouterfranken.arboardgame.old;
//package be.wouterfranken.arboardgame;
//
//public class NonfreeJNILib {
//	static
//	{
//		try {
//			System.loadLibrary("gnustl_shared");
//			System.loadLibrary("opencv_java");
//			System.loadLibrary("nonfree");
//			System.loadLibrary("nonfree_jni");
//		} catch(UnsatisfiedLinkError e) {
//			System.err.println("Native code library failed to load.\n" + e);
//		}
//	}
//	
//	private static boolean openCvLoaded;
//	
//	public static boolean isOpenCVLoaded() {
//		return openCvLoaded;
//	}
//	
//	public static void setOpenCvLoaded(boolean loaded) {
//		openCvLoaded = loaded;
//	}
//	
//	public static native long runDemo(long matPtr);
//	public static native long loadMat();
//}
