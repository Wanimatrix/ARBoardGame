package be.wouterfranken.arboardgame.utilities;

public class TrackingUtilities {
	public static native void compileOCLKernels();
	public static native void nv21ToRGBA(byte[] inData, int width, int height, long outImgPtr);
}
