package be.wouterfranken.arboardgame.app;




public class AppConfig {
	public final static boolean FORCE_TARGET_DESC_RELOAD = true;
	public final static int AMOUNT_PREVIEW_BUFFERS = 5;
	public static boolean TOUCH_EVENT = false;
	
	
	/**
	 * Supported Resolutions:
	 * 
	 * 1600,1200
	 * 1280,720
	 * 960,720
	 * 720,480
	 * 704,576
	 * 640,480
	 * 480,320
	 * 320,240
	 * 176,144
	 */
	public final static int[] PREVIEW_RESOLUTION = new int[]{640,480};
	public final static float ASPECT_RATIO = AppConfig.PREVIEW_RESOLUTION[1]/((float)AppConfig.PREVIEW_RESOLUTION[0]);
	
	/**
	 * Supported FPS ranges (multiplied by 1000)
	 * 
	 * 1000,15000 (1-15)
	 * 1000,30000 (1-30)
	 * 1000,45000 (1-45)
	 * 1000,60000 (1-60)
	 */
	
	public final static int[] FPS_RANGE = new int[]{1000,30000};
}
