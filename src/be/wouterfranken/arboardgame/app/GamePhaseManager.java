package be.wouterfranken.arboardgame.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import be.wouterfranken.arboardgame.gameworld.LegoBrick;
import be.wouterfranken.arboardgame.gameworld.LegoBrickContainer;
import be.wouterfranken.arboardgame.gameworld.Lemming;
import be.wouterfranken.arboardgame.gameworld.LemmingsGenerator;
import be.wouterfranken.arboardgame.gameworld.WorldConfig;
import be.wouterfranken.arboardgame.gameworld.WorldLines;
import be.wouterfranken.arboardgame.rendering.ArRenderer;
import be.wouterfranken.arboardgame.rendering.meshes.MeshObject;
import be.wouterfranken.arboardgame.rendering.meshes.RenderOptions;
import be.wouterfranken.arboardgame.rendering.tracking.BrickTrackerConfig;
import be.wouterfranken.arboardgame.rendering.tracking.CameraPoseTracker;
import be.wouterfranken.arboardgame.rendering.tracking.FrameTrackingCallback;
import be.wouterfranken.arboardgame.rendering.tracking.LegoBrickTracker2;
import be.wouterfranken.arboardgame.utilities.BrickTrackerConfigFactory;
import be.wouterfranken.arboardgame.utilities.Color;
import be.wouterfranken.arboardgame.utilities.ProgressWheel;
import be.wouterfranken.experiments.TimerManager;

public class GamePhaseManager implements AfterLoadingColorCalibListener {
	
	enum Mode { DEBUG, NORMAL};
	
	private final static String TAG = GamePhaseManager.class.getSimpleName();
	private BrickDetectorTask bdt = new BrickDetectorTask();
	private CameraPoseTracker cameraPose;
	private LegoBrickTracker2 legoBrickTracker;
	private LemmingsGenerator lemmingsGenerator;
	private WorldLines w;
	private List<MeshObject> bricksToRender = new ArrayList<MeshObject>();
	private List<MeshObject> lemmingsToRender = new ArrayList<MeshObject>();
	private GamePhase currentGamePhase;
	private Context ctx;
	private CameraView view;
	private AlertDialog.Builder builder;
	private Mode mode;
	
	private ColorCalibration colorCalibration;
	private ArRenderer ar;
	
	private Mat previousAlgoImg;
	
	public enum GamePhase {
		INIT,CALIBRATION,PRE_DETECTION,DETECTION,PRE_PLAY,PLAY;
	}
	
	public GamePhaseManager(CameraPoseTracker cameraPose, CameraView view) {
		this.ar = ar;
		this.colorCalibration = new ColorCalibration(this);
		this.ctx = view.getContext();
		this.view = view;
		builder = new AlertDialog.Builder(ctx);
		this.cameraPose = cameraPose;
		this.legoBrickTracker = new LegoBrickTracker2(ctx);
		w = new WorldLines();
		lemmingsGenerator = new LemmingsGenerator(legoBrickTracker, cameraPose, w);
		this.setGamePhase(GamePhase.INIT);
		previous = currentGamePhase;
		
		builder.setTitle("Mode").setMessage("Do you want to run in normal or debug mode?")
		.setPositiveButton("Debug Mode", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode = Mode.DEBUG;
				
			}
		}).setNegativeButton("Normal Mode", new OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mode = Mode.NORMAL;
				
			}
		}).setCancelable(false);
		
		builder.create().show();
		
		builder = new AlertDialog.Builder(ctx);
	}
	
	private GamePhase previous;
	
	public void setAr(ArRenderer ar) {
		this.ar = ar;
	}
	
	public void frameTick(byte[] frameData, Camera camera) {
		
		long start = System.nanoTime();
		
		if(previous != currentGamePhase) {
			Log.d("LAGGER", "NEW GAME PHASE");
			previous = currentGamePhase;
		} else {
			previous = currentGamePhase;
		}
				
		//		if(previousFrameTime != 0) {
		//			if(frameCount == 0) {
		//				frameWaitAmount = (int) (15*previousFrameTime);
		//			}
		//			frameCount = (frameCount + 1) % frameWaitAmount;
		//		}
				
		//		view.requestRender();
		
		
//		legoBrickTracker.frameTick();
		
//		if(frameCounter > 40+30 && currentGamePhase == GamePhase.CALIBRATION) {
//			Toast.makeText(ctx, "Calibration ends ...", Toast.LENGTH_LONG).show();
//			this.setGamePhase(GamePhase.DETECTION_NEW);
//			frameCounter = -1;
//		}
		
		Size size = camera.getParameters().getPreviewSize();
		long start2 = System.nanoTime();
		Mat colFrameImg = new Mat();
		Mat yuv = new Mat( (int)(size.height*1.5), size.width, CvType.CV_8UC1 );
		yuv.put( 0, 0, frameData );
		Imgproc.cvtColor( yuv, colFrameImg, Imgproc.COLOR_YUV2BGR_NV21, 3);
		if(AppConfig.DEBUG_TIMING) Log.d(TAG, "YUV2RGB (OpenCV) in "+(System.nanoTime()-start2)/1000000L+"ms");
		
		FrameTrackingCallback callback = new FrameTrackingCallback(frameData, camera,start);

		if(AppConfig.CAMERA_POSE_ESTIMATION) { 
			cameraPose.updateCameraPose(colFrameImg, callback);
			Log.d("PROGRAM_FLOW", "CAMERA POSE UPDATED");
			if(!cameraPose.cameraPoseFound()) {
//				camera.addCallbackBuffer(frameData);
				if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Totaltime in "+(System.nanoTime()-start)/1000000L+"ms");
			}
		}
		
		cameraPose.frameTick();
		
		Log.d(TAG, "CameraPose found: "+cameraPose.cameraPoseFound());
		Log.d(TAG, "CurrentPhase: "+currentGamePhase);
		if(cameraPose.cameraPoseFound()) {
			
			Log.d(TAG, "Orientation (in degrees): "+cameraPose.getOrientationDeg()+", cameraPosition: "+Arrays.toString(cameraPose.getCameraPosition()));
			
			switch (currentGamePhase) {
			case CALIBRATION:
//				if(frameCounter == 31) Toast.makeText(ctx, "Calibration starts ...", Toast.LENGTH_LONG).show();
//				if(frameCounter > 30) {
				colorCalibration.colorCalibration(colFrameImg, 500, 0, cameraPose.getMvMat());
//				}
				break;
			case DETECTION:
//				if(frameCounter == 20+40+30+1) Toast.makeText(ctx, "Brick detection starts ...", Toast.LENGTH_LONG).show();
//				if(frameCounter > 20+40+30) {
					detectBricks(colFrameImg, callback);
//				} else {
//					callback.trackingDone(LegoBrickTracker2.class);
//				}
				break;
			case PLAY:
				playGame(callback);
				break;
			default:
				break;
			}
		} else {
			switch (currentGamePhase) {
			case DETECTION:
				callback.trackingDone(LegoBrickTracker2.class);
				break;
			case PLAY:
				callback.trackingDone(LemmingsGenerator.class);
			default:
				break;
			}
		}
		
//		view.requestRender();
		
//		frameCounter++;
	}
	
	private void detectBricks(Mat frame, FrameTrackingCallback callback) {
		Log.d("PROGRAM_FLOW", "CAMERA POSE FOUND");
		if(AppConfig.LEGO_TRACKING) {
//			if(AppConfig.LEGO_TRACKING_CAD){
//				legoBrickTracker.findLegoBrick(frame, cameraPose.getMvMat(), -1, callback, null);
//				CuboidMesh mesh = legoBrickTracker.getTrackedLegoBricks(frame, cameraPose);
//				bricksToRender.add(mesh);
//			} else 
			if(AppConfig.LEGO_TRACKING_LINES) {
				Log.d(TAG, "BDT STATUS: "+bdt.getStatus());
				if(bdt.getStatus() == AsyncTask.Status.FINISHED || bdt.getStatus() == AsyncTask.Status.PENDING) {
					bdt = new BrickDetectorTask();
					bdt.start = System.nanoTime();
					bdt.setupFrameTrackingCallback(callback);
					bdt.setupCameraPoseInfo(cameraPose);
					bdt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, frame);
				}
				Log.d(TAG, "LB2 tracking done.");
				callback.trackingDone(LegoBrickTracker2.class);
			} 
//			else {
//				legoBrickTracker.findLegoBrick(frame, cameraPose.getMvMat(), -1, callback, null);
//			}
		}
	}
	
	private void playGame(FrameTrackingCallback callback) {
		if(AppConfig.LEMMING_RENDERING) {
			long lemmingStart = System.nanoTime();
//			if(AppConfig.PARALLEL_LEMMING_UPDATES) { // NEW LOCKS NECESSARY ON LEMMINGSTORENDER
//				LemmingGeneratorTask lgt = new LemmingGeneratorTask();
//				lgt.setupFrameTrackingCallback(callback);
//				lgt.start = lemmingStart;
//				lgt.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//			} else {
				lemmingsGenerator.frameTick();
				if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming frameUpdate in "+(System.nanoTime()-lemmingStart)/1000000L+"ms");
				callback.trackingDone(LemmingsGenerator.class);
//			}
			lemmingsToRender.clear();
			lemmingsToRender.addAll(lemmingsGenerator.getLemmingMeshes());
		} else if(AppConfig.LEMMING_RENDERING) {
			callback.trackingDone(LemmingsGenerator.class);
		}
	}
	
	
//	private class LemmingGeneratorTask extends AsyncTask<Void, Void, Void> {
//		private FrameTrackingCallback trackingCallback;
//		private long start;
//		
//		@Override
//		protected Void doInBackground(Void... params) {
//			lemmingsGenerator.frameTick();
//			return null;
//		}
//		
//		@Override
//		protected void onPostExecute(Void result) {
//			super.onPostExecute(result);
//			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "Lemming frameUpdate in "+(System.nanoTime()-start)/1000000L+"ms");
//			this.trackingCallback.trackingDone(LemmingsGenerator.class);
//		}
//		
//		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
//			this.trackingCallback = trackingCallback;
//		}
//	}
	
	int count = 0;
	
	private class BrickDetectorTask extends AsyncTask<Mat, Void, List<LegoBrick>> {
		private FrameTrackingCallback trackingCallback;
		private long start;
		private Mat modelView;
		private float orientation;
		
		@Override
		protected void onPreExecute() {
			TimerManager.start("BrickDetection", "Total", BrickTrackerConfigFactory.getConfiguration().toString());
			super.onPreExecute();
		}
		
		@Override
		protected List<LegoBrick> doInBackground(Mat... params) {;
			long startFind = System.nanoTime();
			
			w.resetRemovedBricks();
			try {
				legoBrickTracker.findLegoBrick(params[0], modelView, orientation, colorCalibration.getCalibrationResult(), trackingCallback, w);
			} catch (IOException e) {
				e.printStackTrace();
			}
			Log.d(TAG, "Find brick time: "+(System.nanoTime() - startFind)/1000000.0+"ms");
			
			
			List<LegoBrickContainer> newBrickCand = legoBrickTracker.getNewBrickCandidates();
			
			List<LegoBrick> newCandidateBricks = new ArrayList<LegoBrick>();
			List<LegoBrick> oldAcceptedBricks = new ArrayList<LegoBrick>();
			List<LegoBrick> oldCandidateBricks = new ArrayList<LegoBrick>();
			List<LegoBrick> removed = new ArrayList<LegoBrick>(Arrays.asList(w.getRemovedBricks()));
			
			if(ar != null) {
				oldAcceptedBricks.addAll(w.getBricks());
				for (LegoBrickContainer legoBrickContainer : w.getCandidateBricks()) {
					oldCandidateBricks.addAll(legoBrickContainer);
				}
				oldCandidateBricks.addAll(removed);
				
				for (LegoBrickContainer legoBrickContainer : newBrickCand) {
					newCandidateBricks.addAll(legoBrickContainer);
				}
			}
			
//			Log.d(TAG, "Removed size: "+removed.size());
//			
//			Log.d(TAG, "Removed size: "+removed.size());
			
			w.addBricks(newBrickCand.toArray(new LegoBrickContainer[newBrickCand.size()]), count);
//			bricksToRender.clear();
			List<LegoBrick> bricks = new ArrayList<LegoBrick>();
			
			if(ar != null && AppConfig.DRAW_ALGORITHM) {
				
				if(previousAlgoImg != null ) {
					Mat renderedFrame = ar.getRenderedFrame();
					byte[] renderedData = new byte[(int) (renderedFrame.total()*renderedFrame.channels())];
					ar.getRenderedFrame().get(0, 0, renderedData);
					previousAlgoImg.put(0, 0, renderedData);
					
					Highgui.imwrite("/sdcard/arbg/algo/algoOut"+(count-1)+".png",previousAlgoImg);
				}
				
				int cols = AppConfig.PREVIEW_RESOLUTION[0];
	//			int maxElements = Math.max(Math.max(newCandidateBricks.size(),oldCandidateBricks.size()),oldAcceptedBricks.size());
				int elementsPerRow = (cols-10)/(50+10);
				int newBricksAmountRows = (int)Math.ceil(newCandidateBricks.size()/(double)elementsPerRow);
				int oldBricksAmountRows = (int)Math.ceil(oldAcceptedBricks.size()/(double)elementsPerRow);
				int oldCandBricksAmountRows = (int)Math.ceil(oldCandidateBricks.size()/(double)elementsPerRow);
				
				int rows = AppConfig.PREVIEW_RESOLUTION[1] + (10 + (newBricksAmountRows*(10 + 50)))
								+ (10 + (oldBricksAmountRows*(10 + 50)))
								+ (10 + (oldCandBricksAmountRows*(10 + 50)));
				
//				Mat renderedFrame = ar.getRenderedFrame();
				Mat algoImg = new Mat(rows, cols, CvType.CV_8UC3);
//				Log.d(TAG, "RenderedFrame size: "+renderedFrame.size()+", channels: "+renderedFrame.channels());
				
//				byte[] renderedData = new byte[(int) (renderedFrame.total()*renderedFrame.channels())];
//				ar.getRenderedFrame().get(0, 0, renderedData);
//				algoImg.put(0, 0, renderedData);
				
				List<Point> oldBrickLocations = new ArrayList<Point>();
				
				int currentRow = AppConfig.PREVIEW_RESOLUTION[1] - 50;
				int currentCol = 10;
				int placedBricks = 0;
				for (LegoBrick legoBrick : oldAcceptedBricks) {
					if((placedBricks % elementsPerRow) == 0) {
						currentRow += 60;
						currentCol = 10;
					}
					
					Color c = legoBrick.getColor();
					Core.rectangle(algoImg, new Point(currentCol,currentRow), new Point(currentCol+50,currentRow+50), new Scalar(c.b*255, c.g*255, c.r*255),Core.FILLED);
					oldBrickLocations.add(new Point(currentCol+25,currentRow+25));
					
					currentCol += 60;
					placedBricks++;
				}
				
				if(oldAcceptedBricks.size() != 0) currentRow += 20;
				placedBricks = 0;
				for (LegoBrick legoBrick : oldCandidateBricks) {
					if((placedBricks % elementsPerRow) == 0) {
						currentRow += 60;
						currentCol = 10;
					}
					
					Color c = legoBrick.getColor();
					Core.rectangle(algoImg, new Point(currentCol,currentRow), new Point(currentCol+50,currentRow+50), new Scalar(c.b*255, c.g*255, c.r*255),Core.FILLED);
					Log.d(TAG, "Removed size F: "+removed.size());
					if(removed.contains(legoBrick)) {
						Log.d(TAG, "REMOVED contains this brick");
						Core.line(algoImg, new Point(currentCol+5,currentRow+5), new Point(currentCol+45,currentRow+45), new Scalar(255,255,255), 3);
						Core.line(algoImg, new Point(currentCol+5,currentRow+45), new Point(currentCol+45,currentRow+5), new Scalar(255,255,255), 3);
					}
					oldBrickLocations.add(new Point(currentCol+25,currentRow+25));
					
					currentCol += 60;
					placedBricks++;
				}
				
				List<Pair<Point,Point>> mergeLines = new ArrayList<Pair<Point,Point>>();
				
				if(oldCandidateBricks.size() != 0) currentRow += 20;
				placedBricks = 0;
				for (LegoBrick legoBrick : newCandidateBricks) {
					if((placedBricks % elementsPerRow) == 0) {
						currentRow += 60;
						currentCol = 10;
					}
					
					Color c = legoBrick.getColor();
					Core.rectangle(algoImg, new Point(currentCol,currentRow), new Point(currentCol+50,currentRow+50), new Scalar(c.b*255, c.g*255, c.r*255),Core.FILLED);
					oldBrickLocations.add(new Point(currentCol+25,currentRow+25));
					
					if(legoBrick.getMergedBricksThisFrame().size() == 0) {
//						if(!w.getCandidateBricks().contains(legoBrick)) throw new IllegalStateException("New world candidates must contain this new brick!");
						Core.line(algoImg, new Point(currentCol+25,currentRow+5), new Point(currentCol+25,currentRow+45), new Scalar(255,255,255), 3);
						Core.line(algoImg, new Point(currentCol+5,currentRow+25), new Point(currentCol+45,currentRow+25), new Scalar(255,255,255), 3);
					} else {
						for (LegoBrick lb : legoBrick.getMergedBricksThisFrame()) {
							int idx = oldAcceptedBricks.indexOf(lb);
							if(idx == -1) idx = oldAcceptedBricks.size() + oldCandidateBricks.indexOf(lb);
							if(newCandidateBricks.indexOf(lb) != -1) idx = oldAcceptedBricks.size() + oldCandidateBricks.size() + newCandidateBricks.indexOf(lb);
//							if(oldCandidateBricks.indexOf(lb) == -1) throw new IllegalStateException("Brick with which it was merged was not an old brick!");
							if(idx >= oldBrickLocations.size())
								mergeLines.add(new Pair<Point, Point>(new Point(currentCol+25,currentRow+25), new Point(-1, idx)));
							else
								mergeLines.add(new Pair<Point, Point>(new Point(currentCol+25,currentRow+25), oldBrickLocations.get(idx)));
						}
					}
					
					currentCol += 60;
					placedBricks++;
				}
				
				for (Pair<Point, Point> pair : mergeLines) {
					if(pair.second.x == -1)
						pair = new Pair<Point, Point>(pair.first, oldBrickLocations.get((int)pair.second.y));
					Core.line(algoImg, pair.first, pair.second, new Scalar(255,255,255), 3);
				}
				
				Highgui.imwrite("/sdcard/arbg/renderedFrame.png",ar.getRenderedFrame());
//				Highgui.imwrite("/sdcard/arbg/algo/algoOut"+count+".png",algoImg);
				previousAlgoImg = algoImg;
			}
			
			
//			List<LegoBrickContainer> brickContList = w.getCandidateBricks();
//			long maxMerges = 0;
//			int[] indexes = new int[2];
//			for (int i = 0; i < brickContList.size(); i++) {
//				for (int j = 0; j < brickContList.get(i).size(); j++) {
//					LegoBrick b = brickContList.get(i).get(j);
//					if(b.getMergeCount() > maxMerges) {
//						maxMerges = b.getMergeCount();
//						indexes[0] = i;
//						indexes[1] = j;
//					}
//				}
//			}
//			
//			Log.d(TAG, "Maximum merges: "+maxMerges);
			
//			boolean showCands = false;
			if(mode == Mode.DEBUG && currentGamePhase == GamePhase.DETECTION) {
				List<LegoBrickContainer> brickCont = w.getCandidateBricks();
				for (LegoBrickContainer legoBrickContainer : brickCont) {
					bricks.addAll(legoBrickContainer);
				}
			}
//			else {
				bricks.addAll(w.getBricks());
//				for (LegoBrick legoBrick : bricks) {
//					Log.d(TAG, "Accepted brick CP: "+legoBrick.getCenterPoint()[0]+", "+legoBrick.getCenterPoint()[1]+", "+legoBrick.getCenterPoint()[2]);
//					for (float[] vec : legoBrick.getHalfSideVectors()) {
//						Log.d(TAG, "Accepted brick HV: "+vec[0]+", "+vec[1]+", "+vec[2]);
//					}
//				}
//			}
			
			//w.getBricks();
//			Log.d(TAG, "Active bricks: "+newBrickCand.size());
			
//			for (LegoBrick legoBrick : bricks) {
//				if(legoBrick.getVisibleFrames() > 10 && ((float)legoBrick.getVotes()/legoBrick.getVisibleFrames()) > 0)
//				bricksToRender.add(legoBrick.getMesh(new RenderOptions(true, new Color(1, 0, 0, 1), true)));
//				else {
//					Log.d(TAG, "VisFrames: "+ legoBrick.getVisibleFrames()+ "; VOTES: "+legoBrick.getVotes()+"; RATIO: "+((float)legoBrick.getVotes()/legoBrick.getVisibleFrames()));
//				}
//			}
//			Log.d(TAG, "BricksToRender: "+bricksToRender.size());
			
			Log.d("PROGRAM_FLOW", "BRICKS TO BE RENDERED UPDATED");
			return bricks;
		}
		
		@Override
		protected void onPostExecute(List<LegoBrick> bricks) {
			Log.d(TAG, "POST "+count++);
			TimerManager.stop();
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "1 Brick detection in "+(System.nanoTime()-start)/1000000L+"ms");
			synchronized (bricksToRender) {
				bricksToRender.clear();
				for (LegoBrick legoBrick : bricks) {
					Log.d(TAG, "LegoCOLOR: "+legoBrick.getColor());
					bricksToRender.add(legoBrick.getMesh(new RenderOptions(true, legoBrick.getColor(), false)));
				}
			}
			
			super.onPostExecute(bricks);
			if(AppConfig.DEBUG_TIMING) Log.d(TAG, "2 Brick detection in "+(System.nanoTime()-start)/1000000L+"ms");
//			this.trackingCallback.trackingDone(LegoBrickTracker.class);
		}
		
		private void setupFrameTrackingCallback(FrameTrackingCallback trackingCallback) {
			this.trackingCallback = trackingCallback;
		}
		
		public void setupCameraPoseInfo(CameraPoseTracker camPose) {
			Mat mv = camPose.getMvMat();
			this.modelView = new Mat(mv.size(), mv.type());
			mv.copyTo(modelView);
			this.orientation = camPose.getOrientationDeg();
		}
	}
	
	public List<MeshObject> getBricksToRender() {
		return bricksToRender;
	}
	
	public List<MeshObject> getLemmingsToRender() {
		return lemmingsToRender;
	}
	
	public List<MeshObject> getStarsToRender() {
		return lemmingsGenerator.getStarMeshes();
	}
	
	private void setGamePhase(GamePhase phase) {
		TimerManager.saveAll();
		FrameTrackingCallback.unRegisterAll();
		FrameTrackingCallback.registerTracker(CameraPoseTracker.class);
		switch (phase) {
			case DETECTION:
				FrameTrackingCallback.registerTracker(LegoBrickTracker2.class);
				break;
			case PLAY:
				FrameTrackingCallback.registerTracker(LemmingsGenerator.class);
				break;
			default:
				break;
		}
		
		this.currentGamePhase = phase;
		Toast.makeText(ctx, phase+" phase started", Toast.LENGTH_SHORT).show();
	}
	
	public void goToNextPhase() {
		
		Log.d("LAGGER", "GOTO NEXT PHASE");
		Log.d(TAG, "CurrentPhase: "+currentGamePhase);
//		Toast.makeText(ctx, currentGamePhase+" phase started", Toast.LENGTH_SHORT).show();
//		Toast.makeText(ctx, currentGamePhase+" phase ended", Toast.LENGTH_LONG).show();
		switch(currentGamePhase) {
			case INIT:
				
				builder.setTitle("Init phase")
				.setMessage("Are you sure you want to start calibrating?")
				.setPositiveButton("Yes", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						setGamePhase(GamePhase.CALIBRATION);
					}
				})
				.setNegativeButton("Cancel", new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {}
				}).create().show();
				builder = new Builder(ctx);
				
				
				break;
			case CALIBRATION:
				setGamePhase(GamePhase.PRE_DETECTION);
				beforeDetection(true);
				
				break;
			case PRE_DETECTION:
				if(view.getVRMode()) view.setVRModeEnabled(false);
				count = 0;
				// Clean algo folder!
				File dir = new File("/sdcard/arbg/algo"); 
				if (dir.isDirectory()) {
			        String[] children = dir.list();
			        for (int i = 0; i < children.length; i++) {
			            new File(dir, children[i]).delete();
			        }
			    }
				setGamePhase(GamePhase.DETECTION);
				break;
			case DETECTION:
				setGamePhase(GamePhase.PRE_PLAY);
			case PRE_PLAY:
				
				bdt.cancel(false);
				
				AsyncTask<Void, Void, Void> resetBricksToRender = new AsyncTask<Void, Void, Void>() {
					ProgressWheel wheel = new ProgressWheel(ctx);
					
					@Override
					protected void onPreExecute() {
						wheel.setBarColor(android.graphics.Color.BLUE);
						wheel.spin();
						RelativeLayout layout = (RelativeLayout)view.getParent();
						layout.addView(wheel);
						view.setVisibility(View.INVISIBLE);
//						layout.getChildAt(1).setVisibility(View.VISIBLE);
					}
					
					@Override
					protected Void doInBackground(Void... params) {
//						Log.d(TAG, "TheWorld: "+w);
						bricksToRender.clear();
//						w.clean();
						Log.d(TAG, "World has "+ w.getBrickAmount()+"bricks. ");
						w.activateAllBricks();
						Mat mergeDbg = new Mat((int)(AppConfig.BOARD_SIZE[1]*100),(int)(AppConfig.BOARD_SIZE[0]*100),CvType.CV_8UC3);
						List<List<MatOfPoint>> brickPlanes = new ArrayList<List<MatOfPoint>>();
						brickPlanes.add(new ArrayList<MatOfPoint>());
						brickPlanes.add(new ArrayList<MatOfPoint>());
						brickPlanes.add(new ArrayList<MatOfPoint>());
						brickPlanes.add(new ArrayList<MatOfPoint>());
						
						List<List<LegoBrick>> brickFrames = new ArrayList<List<LegoBrick>>();
						brickFrames.add(new ArrayList<LegoBrick>());
						brickFrames.add(new ArrayList<LegoBrick>());
						brickFrames.add(new ArrayList<LegoBrick>());
						brickFrames.add(new ArrayList<LegoBrick>());
						
						for (LegoBrick brick : w.getBricks()) {
							int zIdx = Math.round((brick.getCenterPoint()[2]-0.95f/2)/0.95f);
							Log.d(TAG, "Z: "+zIdx+", "+brick.getCenterPoint()[2]);
							float[][] cuboid = brick.getCuboid();
							MatOfPoint plane = new MatOfPoint(
									new Point(cuboid[0][0]*100+AppConfig.BOARD_SIZE[0]/2.0*100,-1*cuboid[0][1]*100+AppConfig.BOARD_SIZE[1]/2.0*100),
									new Point(cuboid[1][0]*100+AppConfig.BOARD_SIZE[0]/2.0*100,-1*cuboid[1][1]*100+AppConfig.BOARD_SIZE[1]/2.0*100),
									new Point(cuboid[2][0]*100+AppConfig.BOARD_SIZE[0]/2.0*100,-1*cuboid[2][1]*100+AppConfig.BOARD_SIZE[1]/2.0*100),
									new Point(cuboid[3][0]*100+AppConfig.BOARD_SIZE[0]/2.0*100,-1*cuboid[3][1]*100+AppConfig.BOARD_SIZE[1]/2.0*100));
							brickPlanes.get(zIdx).add(plane);
							brickFrames.get(zIdx).add(brick);
							Log.d(TAG, "Brick center: "+brick.getCenterPoint()[0]+", "+brick.getCenterPoint()[1]+", "+brick.getCenterPoint()[2]+" of brick "+brick.toString());
						}
						for (int i = 0; i < brickPlanes.size(); i++) {
							Scalar color;
							switch (i) {
							case 0:
								color = new Scalar(0,0,255);
								break;
							case 1:
								color = new Scalar(0, 255, 0);
								break;
							case 2:
								color = new Scalar(255, 0, 0);
								break;
							case 3:
								color = new Scalar(255, 255, 0);
								break;
							default:
								color = new Scalar(255, 255, 255);
								break;
							}
							Imgproc.drawContours(mergeDbg, brickPlanes.get(i), -1, color);
							for (int j = 0; j < brickPlanes.get(i).size(); j++) {
								LegoBrick b = brickFrames.get(i).get(j);
								Point origin = new Point(b.getCenterPoint()[0]*100+AppConfig.BOARD_SIZE[0]/2.0*100,
										-1*b.getCenterPoint()[1]*100+AppConfig.BOARD_SIZE[1]/2.0*100);
								Log.d(TAG, "Put added frame "+brickFrames.get(i).get(j).getAddedFrame()+" on location "+origin+" with original cp {"
										+b.getCenterPoint()[0]+" , "+b.getCenterPoint()[1]+" }");
								Core.putText(mergeDbg, ""+brickFrames.get(i).get(j).getAddedFrame(), origin, Core.FONT_HERSHEY_PLAIN, 0.6f, color);
								origin.y = origin.y + 10;
								Core.putText(mergeDbg, ""+brickFrames.get(i).get(j).getAcceptedFrame(), origin, Core.FONT_HERSHEY_PLAIN, 0.6f, color);
							}
						}
						
						Highgui.imwrite("/sdcard/arbg/brickGround.png", mergeDbg);
						lemmingsGenerator.generatePath();
						for (LegoBrick legoBrick : w.getBricks()) {
							bricksToRender.add(legoBrick.getMesh(new RenderOptions(true, legoBrick.getColor(), false)));
						}
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result) {
						super.onPostExecute(result);
						
						wheel.setVisibility(View.INVISIBLE);
						view.setVisibility(View.VISIBLE);
						
//						RelativeLayout layout = (RelativeLayout)view.getParent();
//						layout.getChildAt(1).setVisibility(View.INVISIBLE);
						
						builder.setMessage("Your world was succesfully scanned. Please press play and put on your cardboard.")
						   .setTitle("Pre-play Phase")
						   .setPositiveButton("Play", 
								   new OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {
//											goToNextPhase();
											if(!view.getVRMode())view.setVRModeEnabled(true);
											setGamePhase(GamePhase.PLAY);
										}
						   		   });
						builder.create().show();
					}
					
				};
				
				resetBricksToRender.execute();
				
				break;
			case PLAY:
				beforeDetection(false);
				w = new WorldLines();
				lemmingsGenerator = new LemmingsGenerator(legoBrickTracker, cameraPose, w);
				lemmingsToRender.clear();
				setGamePhase(GamePhase.PRE_DETECTION);
				break;
			default:
				break;
		}
		Log.d(TAG, "CurrentPhase: "+currentGamePhase);
		
		Log.d("LAGGER", "GOTO NEXT PHASE DONE");
	}
	
	public ColorCalibration getColorCalibration() {
		return colorCalibration;
	}
	
	public boolean canEditBrickDetectionParams() {
		return currentGamePhase.ordinal() == GamePhase.DETECTION.ordinal();
	}
	
	public boolean canSaveColorCalibration() {
		return currentGamePhase.ordinal() > GamePhase.CALIBRATION.ordinal();
	}
	
	public void beforeDetection(boolean calibration) {
		String message = "";
		if(calibration) {
			message += "Please remove all calibration blocks and build your world. ";
		} else {
			message += "Please build a world. ";
		}
		message += "After you are done, click \"Scan world\" to start scanning the world.";
		builder.setMessage(message)
		   .setTitle("Pre-detection Phase")
		   .setPositiveButton("Scan world", 
				   new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							goToNextPhase();
						}
		   		   });
		builder.create().show();
	}

	@Override
	public void afterLoadingColorCalib() {
		setGamePhase(GamePhase.PRE_DETECTION);
		beforeDetection(false);
	}
}
