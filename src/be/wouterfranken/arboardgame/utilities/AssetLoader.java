package be.wouterfranken.arboardgame.utilities;

import java.io.File;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;

public class AssetLoader {
	private Context context;
	
	private static AssetLoader assetLoader = new AssetLoader();
	
	public AssetLoader() {}
	
	public static void setContext(Context context) {
		assetLoader.context = context;
	}
	
	public static AssetLoader getInstance() {
		if(assetLoader.context == null)
			throw new IllegalStateException("Calling assetLoader before context is set!");
		return assetLoader;
	}
	
	public Mat loadImgFromAssets(String name) {
		File f = new File(context.getCacheDir()+"/"+name);
		
		if (!f.exists()) try {
			InputStream is = context.getAssets().open(name);
			
			Bitmap bmp = BitmapFactory.decodeStream(is);
			Mat img = new Mat();
			Utils.bitmapToMat(JPEGtoRGB888(bmp), img);
			
			return img;
		} catch (Exception e) {
			throw new RuntimeException(e); 
		}
		
		return null;
	}
	
	public Mat loadMatFromAssets(String name) {
		File f = new File(context.getCacheDir()+"/"+name);
		
		if (!f.exists()) try {
			InputStream is = context.getAssets().open(name);
			
			Bitmap bmp = BitmapFactory.decodeStream(is);
			Mat img = new Mat();
			Utils.bitmapToMat(JPEGtoRGB888(bmp), img);
			
			return img;
		} catch (Exception e) {
			throw new RuntimeException(e); 
		}
		
		return null;
	}
	
	private Bitmap JPEGtoRGB888(Bitmap img) 
    { 
        int numPixels = img.getWidth()* img.getHeight(); 
        int[] pixels = new int[numPixels]; 

        //Get JPEG pixels.  Each int is the color values for one pixel. 
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight()); 

        //Create a Bitmap of the appropriate format. 
        Bitmap result = Bitmap.createBitmap(img.getWidth(), img.getHeight(), Config.ARGB_8888); 

        //Set RGB pixels. 
        result.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight()); 
        return result; 
    } 
}
