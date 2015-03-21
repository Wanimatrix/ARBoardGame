/*
 * jni_interface.cpp
 *
 *  Created on: 5-okt.-2014
 *      Author: Wouter Franken
 */
// Libraries (C,OpenCV,Android,Aruco,OpenMP,...)
#include <logger.hpp>
#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <android/log.h>
#include <iostream>
#include <string>
#include <limits.h>
#include <stdio.h>
#include <aruco.h>
#include <math.h>
#include <algorithm>
#include <vector>
#include <omp.h>

// App includes
#include "utilities.hpp"
#include "app.hpp"
#include "detect.hpp"
 #include "brickDetectorLines.hpp"

extern "C" {
	#include "getRealTime.h"
}

#define APPNAME "be.wouterfranken.arboardgame"
#define DEBUG 1
#define TIMING 0
#define DRAW_MARKERS 0 /*Write markers on image*/
#define LARGE_CONTOURS 0 /*Write contours on big (non thresholded) image*/
#define SMALL_CONTOURS 0 /*Write contours on downsampled image*/

#define WRITE_CONTOURS(small) small ? SMALL_CONTOURS : LARGE_CONTOURS

using namespace cv;
using namespace aruco;

extern CalibrationData calibData;

extern "C"
{
	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_saveMat(
			JNIEnv * env, jclass javaClass, jstring filePath, jstring matName, jlong matPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_loadMatFromFile(
			JNIEnv * env, jclass javaClass, jstring filePath, jstring matName, jlong matPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_loadCameraCalibration
		  (JNIEnv * env, jobject jobject, jstring cameraIntDistPath, jlong cameraMat, jlong distortion);

	JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_getCameraPose
	  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong projMatPtr, jlong mvMatPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_getCalibrationData
		  (JNIEnv *env, jobject jobject, jlong cameraMat, jlong distortion);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrick
		  (JNIEnv *env, jobject object, jlong yuvFrameImagePtr, jlong contourPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrick2
			(JNIEnv *env, jobject object, jlong bgrPointer, jlong thresholdPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_get2DPointsFrom3D
			(JNIEnv *env, jobject object, jlong points3dPtr, jlong glMvPtr, jlong intrinsicsPtr, jlong points2dPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrick3
			(JNIEnv *env, jobject object, jlong bgrPointer, jlong resultMatPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_generateHOGDescriptors
			(JNIEnv *env, jobject object, jstring renderImgsPath);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrickLines
			(JNIEnv *env, jobject object, jlong bgrPointer, jfloat upAngle, jlong resultMatPtr, jlong origContMatPtr);

	JNIEXPORT jfloatArray JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_checkOverlap
			(JNIEnv *env, jobject object, jlong inputPoints, jint idx);

	JNIEXPORT jfloatArray JNICALL Java_be_wouterfranken_arboardgame_gameworld_LegoBrick_checkCurrentOverlap
			(JNIEnv *env, jobject object, jlong inputPoints);

	JNIEXPORT jfloatArray JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_getOverlap
			(JNIEnv *env, jobject object, jlong bricksPointer);
}

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_loadCameraCalibration(
				JNIEnv * env, jobject jobject, jstring cameraIntDistPath, jlong cameraMat, jlong distortion)
{
	const char * path = env->GetStringUTFChars(cameraIntDistPath, NULL);

	Utilities::loadCameraCalibration(path, calibData.intrinsics, calibData.distortion);
	env->ReleaseStringUTFChars(cameraIntDistPath, path);
#if DEBUG
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Intrinsics: %d",calibData.intrinsics.cols*calibData.intrinsics.rows);
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Distortion size: %d",calibData.distortion.cols*calibData.distortion.rows);

	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Camera calibration done...");
#endif
	*(Mat *) cameraMat = calibData.intrinsics;
	*(Mat *) distortion = calibData.distortion;
}

bool markerSort(aruco::Marker i, aruco::Marker j) {
	float sumI = 0, sumJ = 0;

	for (int k = 0; k < 4; ++k) {
		sumI += sqrt(pow(i[(k+1)%4].x - i[k].x,2)+pow(i[(k+1)%4].y - i[k].y,2));
		sumJ += sqrt(pow(j[(k+1)%4].x - j[k].x,2)+pow(j[(k+1)%4].y - j[k].y,2));
	}
	return sumI >= sumJ;
}

int prevMarkerId = 0;

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_getCalibrationData
		  (JNIEnv *env, jobject jobject, jlong cameraMat, jlong distortion) {
	*(Mat *) cameraMat = calibData.intrinsics;
	*(Mat *) distortion = calibData.distortion;
}

JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_getCameraPose
  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong projMatPtr, jlong mvMatPtr) {

	Mat *mv = (Mat *) mvMatPtr;
	Mat *proj = (Mat *) projMatPtr;
	Mat *frameImage = (Mat *) frameImagePtr;

	double start = getRealTime();
	float markerSize = 5.9f;

	aruco::MarkerDetector detector;
	std::vector<aruco::Marker> markers;
	aruco::CameraParameters camParams(calibData.intrinsics,calibData.distortion,calibData.imgSize);
	Size inputImgSize = frameImage->size();

	camParams.resize(inputImgSize);
	Utilities::logMat(camParams.CameraMatrix,"JNI_Intrinsics");
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Camparams initialized in %f ms...",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	detector.pyrDown(2);
	detector.setThresholdMethod(aruco::MarkerDetector::ADPT_THRES);
	detector.setCornerRefinementMethod(aruco::MarkerDetector::SUBPIX);
	detector.setMinMaxSize(0.04f,1.0f);
//	detector.enableErosion(true);
//	detector.setDesiredSpeed(2);
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Detector was set in %f ms...",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	detector.detect(*frameImage,markers,camParams.CameraMatrix,camParams.Distorsion,markerSize);
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Detection done in %f ms...",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	sort(markers.begin(),markers.end(),markerSort);
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Markers sorted in %f ms...",((float)(getRealTime() - start))*1000.0);
#endif
#if DEBUG
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Marker detection done...");
#endif

	if(markers.size() > 0) {
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Marker detected");
#endif

		double arucoProj[16];
		double arucoMv[16];

		int corner;
//		Point2f cornerToOrigin;
		Point2i edge;
//		Point_<Point2f> edgeToOrigin = Point_<Point2f>(Point2f(),Point2f());
		int inner;
//		Point2f innerToOrigin;
		Point2f innerToCenter;
		Point2f outerToCenter;
		Point2i quadrant;

		aruco::Marker m;
		cv::Mat ObjPoints = cv::Mat(4,1,CV_32FC3);
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Variables initialized ...");
#endif

		m = aruco::Marker(markers[0]);

#if DRAW_MARKERS
		Mat drawing = Mat();
		cvtColor(*frameImage,drawing,CV_GRAY2BGR);
		m.draw(drawing,Scalar(0,255,0),1);
		imwrite("/sdcard/nonfree/markers.png",drawing);
#endif
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"aruco marker was set ...");
#endif
#if TIMING
		start = getRealTime();
#endif
		switch(markers[0].id) {
		case 1:
			edge.x = 1;
			corner = 0;
			innerToCenter = Point2f(3.2f,7.85f);
			outerToCenter = Point2f(9.1f,13.75f);
			quadrant.x = -1;
			quadrant.y = 1;
			break;
		case 11:
			edge.x = 0;
			corner = 1;
			innerToCenter = Point2f(3.9f,7.85f);
			outerToCenter = Point2f(9.8f,13.75f);
			quadrant.x = 1;
			quadrant.y = 1;
			break;
		case 111:
			edge.x = 3;
			corner = 2;
			innerToCenter = Point2f(3.9f,8.05f);
			outerToCenter = Point2f(9.8f,13.95f);
			quadrant.x = 1;
			quadrant.y = -1;
			break;
		case 1011:
			edge.x = 2;
			corner = 3;
			innerToCenter = Point2f(3.2f,8.05f);
			outerToCenter = Point2f(9.1f,13.95f);
			quadrant.x = -1;
			quadrant.y = -1;
			break;
		}
		edge.y = (edge.x + 2) % 4;
		inner = (corner + 2) % 4;
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Index variables were set ...");
#endif

//		double halfSize=markerSize/2.;
//		float innerToCenter = 3.9f;
//		float outerToCenter = markerSize+innerToCenter;
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Coordinate system variables were set ...");
#endif

		ObjPoints.at<Vec3f>(corner,0)=cv::Vec3f(quadrant.x*outerToCenter.x,quadrant.y*outerToCenter.y,0);
		ObjPoints.at<Vec3f>(edge.x,0)=cv::Vec3f(quadrant.x*innerToCenter.x,quadrant.y*outerToCenter.y,0);
		ObjPoints.at<Vec3f>(inner,0)=cv::Vec3f(quadrant.x*innerToCenter.x,quadrant.y*innerToCenter.y,0);
		ObjPoints.at<Vec3f>(edge.y,0)=cv::Vec3f(quadrant.x*outerToCenter.x,quadrant.y*innerToCenter.y,0);

#if TIMING
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"ObjectPoints found in %f ms...",((float)(getRealTime() - start))*1000.0);
#endif
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"ObjPoints initialized...");
#endif

#if TIMING
		start = getRealTime();
#endif
		camParams.glGetProjectionMatrix(inputImgSize,inputImgSize,arucoProj,0.1,10000,false);
#if TIMING
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"projection matrix calculated in %f ms...",((float)(getRealTime() - start))*1000.0);
#endif
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"ProjectionMatrix calculated...");
#endif

#if TIMING
		start = getRealTime();
#endif
		m.calculateExtrinsics(markerSize,camParams.CameraMatrix,cv::Mat(),ObjPoints,false);
#if TIMING
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Extrinsics calculated in %f ms...",((float)(getRealTime() - start))*1000.0);
#endif
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Extrinsics calculated...");
#endif
#if TIMING
		start = getRealTime();
#endif
		m.glGetModelViewMatrix(arucoMv);
#if TIMING
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Modelview matrix found in %f ms...",((float)(getRealTime() - start))*1000.0);
#endif
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"ModelviewMatrix calculated...");
#endif
#if TIMING
		start = getRealTime();
#endif
		// OpenGL matrices to Mat
		Mat arucoProjMat = Mat::zeros(4,4,CV_64F);
		Mat arucoMvMat = Mat::zeros(4,4,CV_64F);
		for (int i = 0; i < 16; ++i) {
				arucoProjMat.at<double>(i/4,i%4) = arucoProj[i];
				arucoMvMat.at<double>(i/4,i%4) = arucoMv[i];
		}
#if TIMING
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"penGL arrays to OpenCV Mat conversion done in %f ms...",((float)(getRealTime() - start))*1000.0);
#endif
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"OpenGL arrays to OpenCV Mat conversion done...");
#endif

		*proj = arucoProjMat;
		*mv = arucoMvMat;
	} else {
		*mv = cv::Mat();
		*proj = cv::Mat();
	}

	return true;
}


// TODO: FIX BUGS WHEN YELLOW + BLUE ARE ENABLED
enum Color {
	RED,
	YELLOW,
	BLUE, // Add extra colors here + in the array below!


	COLOR_ITEM_AMOUNT};
struct HSVColorBounds {
	Scalar lower;
	Scalar upper;
	int close_kernel_size;
	int open_kernel_size;
};

struct HSVColorBounds hsvColors[COLOR_ITEM_AMOUNT] =
	{
		{Scalar(160,153,30),Scalar(180,255,255), 5, 0}, // RED
		{Scalar(135,147,30),Scalar(160,255,255), 9, 0}, // YELLOW
		{Scalar(0,42,13),Scalar(112,255,255), 0, 17} // BLUE
	};

	// Hue (0,112), Sat (42,255), Val (13,255), Close Kernel Size 0, Open Kernel Size 17

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrick(JNIEnv *env, jobject object, jlong bgrPointer, jlong contourPtr) {
	Mat bgr_tmp = *(Mat *)bgrPointer;
	Mat *contour = (Mat *)contourPtr;
#if DEBUG
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Jnicall LegoBrickTracker ...");
#endif
	Mat bgr_down = Mat();
	Mat hsv = Mat();
	Mat bgr = Mat();
	bgr_tmp.copyTo(bgr);

#if WRITE_CONTOURS(false)
	Mat outImg;
	bgr.copyTo(outImg);
#endif
	double start;
#if TIMING
	start = getRealTime();
#endif
	int pyrlvl = 0;

	bgr_down = bgr;
	for(int i = 0;i<pyrlvl;i++) {
		Mat tmp;
		pyrDown(bgr_down,tmp);
		bgr_down = tmp;
	}

//	bgr_down = bgr;
//	int blockAmount = 4;
//	int colBlocks = blockAmount/4;
//	int rowblocks = blockAmount/colBlocks;
//	for (int i = 0; i < pyrlvl; ++i) {
//		Mat tmpDown(Size(bgr_down.cols/2,bgr_down.rows/2),bgr_down.type());
//		#pragma omp parallel for
//		for (int block = 0; block < blockAmount; ++block) {
//			int colBegin = (bgr_down.cols/colBlocks)*(block%colBlocks);
//			int colAmount = (bgr_down.cols/colBlocks)*(block%colBlocks+1)-colBegin;
//			int rowBegin = (bgr_down.rows/rowblocks)*(block/colBlocks);
//			int rowAmount = (bgr_down.rows/rowblocks)*(block/colBlocks+1)-rowBegin;
//			Mat bgrDownBlock(bgr_down, cv::Rect(colBegin, rowBegin, colAmount, rowAmount));
//
//			colBegin = (tmpDown.cols/colBlocks)*(block%colBlocks);
//			colAmount = (tmpDown.cols/colBlocks)*(block%colBlocks+1)-colBegin;
//			rowBegin = (tmpDown.rows/rowblocks)*(block/colBlocks);
//			rowAmount = (tmpDown.rows/rowblocks)*(block/colBlocks+1)-rowBegin;
//			Mat tmpDownBlock(tmpDown, cv::Rect(colBegin, rowBegin, colAmount, rowAmount));
//
//			Mat tmpBlock;
//			tmpDownBlock.copyTo(tmpBlock);
//
//			pyrDown(bgrDownBlock,tmpBlock);
//			tmpBlock.copyTo(tmpDownBlock);
//		}
//		bgr_down = tmpDown;
//	}
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Pyrdown time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif


#if TIMING
	start = getRealTime();
#endif
	cvtColor( bgr_down, hsv, COLOR_RGB2HSV_FULL);

	vector<Mat> thresholded(sizeof(hsvColors)/sizeof(*hsvColors)); //Mat(Size(bgr_down.size().width,bgr_down.size().height),CV_8UC1)

	int N = 5;
	Mat kernel = getStructuringElement(MORPH_ELLIPSE, Point(N, N));
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Conversion RGB-HSV time: %f\n",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	// Mat redThresh;
	// Mat yellowThresh;
	// Mat blueThresh;
	for (int i = 0; i < sizeof(hsvColors)/sizeof(*hsvColors); ++i)
	{
		inRange(hsv, hsvColors[i].lower,hsvColors[i].upper,thresholded[i]);
	}
	// bitwise_or(redThresh,yellowThresh,thresholded);

#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Threshold time: %f\n",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	Mat threshInv;
	for (int i = 0; i < thresholded.size(); ++i)
	{
		bitwise_not(thresholded[i],threshInv);
		morphologyEx(threshInv, thresholded[i], MORPH_CLOSE, kernel);
		bitwise_not(thresholded[i],thresholded[i]);
	}
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Closing time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif

	vector<Mat> response(thresholded.size());
	vector<vector<vector<Point> > > contours(thresholded.size());
	vector<vector<Vec4i> > hierarchy(thresholded.size());
	vector <KeyPoint> kpts;

#if WRITE_CONTOURS(true)
	Mat thresholdImgColor;
	cvtColor(thresholded[RED],thresholdImgColor,COLOR_GRAY2BGR);
#endif
#if TIMING
	start=getRealTime();
#endif
	/// Detect edges using canny
	for (int col = 0; col < thresholded.size(); ++col) Canny( thresholded[col], response[col], 100, 100*2, 3);
	// Canny( yellowThresh, responseYellow, 100, 100*2, 3);
	// Canny( blueThresh, responseBlue, 100, 100*2, 3);
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Canny edge detection time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif
	/// Find contours
	vector<vector<vector<Point> > > contours0(contours.size());
#if TIMING
	start=getRealTime();
#endif
	for (int col = 0; col < response.size(); ++col) {
		findContours( response[col], contours0[col], hierarchy[col], CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );
		contours[col].resize(contours0[col].size());
	}

	// findContours( responseYellow, contours0[1], hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );
	// contours[1].resize(contours0[1].size());
	// findContours( responseBlue, contours0[2], hierarchy, CV_RETR_EXTERNAL, CV_CHAIN_APPROX_SIMPLE, Point(0, 0) );
	// contours[2].resize(contours0[2].size());
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Finding contours time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif

	double maxArea = 0;
#if TIMING
	start = getRealTime();
#endif
	for( int c = 0; c< contours0.size(); c++ ) {
		for( int i = 0; i< contours0[c].size(); i++ )
		{
			double epsilon = 0.01*arcLength(contours0[c][i],true);
			approxPolyDP(Mat(contours0[c][i]), contours[c][i], epsilon, true);
			for (float fract = 0.02f; contours[c][i].size() > 6; fract += 0.01) {
				epsilon = fract*arcLength(contours0[c][i],true);
				approxPolyDP(Mat(contours0[c][i]), contours[c][i], epsilon, true);
			}
			double area = contourArea(contours[c][i]);

			if(area > maxArea)
				maxArea = area;
#if WRITE_CONTOURS(true)
			Scalar color = Scalar(rand() % 256,rand() % 256,rand() % 256);
			drawContours( thresholdImgColor, contours[c], i, color, 2, 8, hierarchy[c], 0, Point() );
#endif
		}
	}
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Approxpoly time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif

	float red_den= pow(2.0f,pyrlvl);
	float offInc= ((pyrlvl/2.0) - 0.5);
#if TIMING
	start = getRealTime();
#endif

	int totalAmountContours = 0;
	for( int col = 0; col< contours.size(); col++ ) {
		std::vector<std::vector<Point> >::iterator i = contours[col].begin();
		while(i != contours[col].end()) {
			if(contourArea(*i) < maxArea/100.0) {
				i = contours[col].erase(i);
			}
			else {
				for (int c = 0; c < i->size(); ++c) {
					(*i)[c].x = (*i)[c].x*red_den+offInc;
					(*i)[c].y = (*i)[c].y*red_den+offInc;
				}
				totalAmountContours++;
	#if WRITE_CONTOURS(false)
				// Scalar color = Scalar(rand() % 256,rand() % 256,rand() % 256);
				// drawContours( outImg, contours[col], i, color, 2, 8, hierarchy[col], 0, Point() );
	#endif

				i++;
			}
		}
	}
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Contour filtering time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif
#if WRITE_CONTOURS(true)
	imwrite("/sdcard/arbg/threshimg.png",thresholdImgColor);
#endif
#if WRITE_CONTOURS(false)
	imwrite("/sdcard/arbg/threshimg.png",outImg);
#endif
	int emptyContourColors = 0;
	int rowNb = 0;
	Mat contourMat = Mat::zeros(totalAmountContours,12+1+1,CV_32SC1);
	for( int col = 0; col< contours.size(); col++ ) {
		if(contours[col].size()>0) {
			for (int j = 0; j < contours[col].size(); ++j) {
				contourMat.at<int>(rowNb,0) = col;
				contourMat.at<int>(rowNb,1) = contours[col][j].size();
				for (int i = 0; i < contours[col][j].size()*2; i+=2) {
					contourMat.at<int>(rowNb,i+2) = contours[col][j][i/2].x;
					contourMat.at<int>(rowNb,i+3) = contours[col][j][i/2].y;
				}
				rowNb++;
			}
		}
		else {
			emptyContourColors++;
		}
	}

	if(emptyContourColors == COLOR_ITEM_AMOUNT) {
		*contour = Mat();
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Result: No bricks FOUND");
#endif
	} else {
		*contour = contourMat;
		Utilities::logMat(*contour,"Contours");
#if DEBUG
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Result: bricks FOUND");
#endif
	}
}

void morphology_operations(Mat src, Mat dst);

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrick2(JNIEnv *env, jobject object, jlong bgrPointer, jlong thresholdPtr) {
	Mat bgr_tmp = *(Mat *)bgrPointer;
	Mat *thresholded = (Mat *)thresholdPtr;
#define TAG "LegoBrickJNI"
#if DEBUG
	__android_log_print(ANDROID_LOG_DEBUG,TAG,"Jnicall LegoBrickTracker 2...");
#endif
	Mat hsv = Mat();
	Mat bgr = Mat();
	bgr_tmp.copyTo(bgr);

	double start;

#if TIMING
	start = getRealTime();
#endif
	// imwrite("/sdcard/arbg/bgr.png",bgr);
	cvtColor( bgr, hsv, COLOR_RGB2HSV_FULL);
	// imwrite("/sdcard/arbg/hsv.png",hsv);

	*thresholded = Mat(Size(bgr.size().width,bgr.size().height),CV_8UC1);

	// int N = 5;
	// Mat kernel = getStructuringElement(MORPH_ELLIPSE, Point(N, N));

#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,TAG,"Conversion RGB-HSV time: %f\n",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	inRange(hsv, hsvColors[0].lower, hsvColors[0].upper,*thresholded);

	for (int i = 1; i < sizeof(hsvColors)/sizeof(*hsvColors); ++i) //
	{
		Mat tmp;
		inRange(hsv, hsvColors[i].lower,hsvColors[i].upper,tmp);

		// if( i == (sizeof(hsvColors)/sizeof(*hsvColors))-1) {
		// 	imwrite("/sdcard/arbg/thresholdedTmp.png", tmp);
		// }

		// morphology_operations(tmp, tmp, i);

		bitwise_or(*thresholded,tmp,*thresholded);
	}


	#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,TAG,"Threshold time: %f\n",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
	#endif
	Mat threshInv;

	// imwrite("/sdcard/arbg/thresholded.png",*thresholded);

	morphology_operations(*thresholded, *thresholded);

	// imwrite("/sdcard/arbg/closed.png",*thresholded);
	// bitwise_not(*thresholded,threshInv);
	// morphologyEx(threshInv, *thresholded, MORPH_CLOSE, kernel);
	// bitwise_not(*thresholded,*thresholded);

	#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,TAG,"Closing time: %f\n",((float)(getRealTime() - start))*1000.0);
	#endif

	imwrite("/sdcard/arbg/thresholded.png", *thresholded);
	imwrite("/sdcard/arbg/bgr.png", bgr);
}

void morphology_operations(Mat src, Mat dst) {
	morphologyEx(src, src, MORPH_CLOSE, getStructuringElement(MORPH_ELLIPSE,
		Point(4,4)));
	morphologyEx(dst, dst, MORPH_OPEN, getStructuringElement(MORPH_ELLIPSE,
		Point(5,5)));
	// bitwise_not(src,dst);
	// if(hsvColors[i].close_kernel_size > 0) {
		// morphologyEx(dst, dst, MORPH_CLOSE, getStructuringElement(MORPH_ELLIPSE,
		// Point(9,9)));
	// }
	// if(hsvColors[i].open_kernel_size > 0) {
		// morphologyEx(dst, dst, MORPH_OPEN, getStructuringElement(MORPH_ELLIPSE,
		// Point(17,17)));
	// }
	// bitwise_not(dst,dst);
}


JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPoseTracker_get2DPointsFrom3D
	(JNIEnv *env, jobject object, jlong points3dPtr, jlong glMvPtr, jlong intrinsicsPtr, jlong points2dPtr) {
	Mat points3d = *(Mat *) points3dPtr;
	Mat glMv = *(Mat *) glMvPtr;
	Mat *points2d = (Mat *) points2dPtr;
	Mat intrinsics = *(Mat *) intrinsicsPtr;
	double start = getRealTime();

	double startTranspose = getRealTime();
	Mat glMvTransposed;
	transpose(glMv, glMvTransposed);
	// Utilities::logMat(glMvTransposed,"MV");
	__android_log_print(ANDROID_LOG_DEBUG,"2D3DTime","Transpose time: %f\n",((float)(getRealTime() - startTranspose))*1000.0);

	// Set Extrinsics
	double startExtrinsics = getRealTime();
	Mat extrinsics = Mat::zeros(3,4, CV_32FC1);
    glMvTransposed.row(0).copyTo(extrinsics.row(0));
    glMvTransposed.row(1).copyTo(extrinsics.row(1));
    __android_log_print(ANDROID_LOG_DEBUG,"2D3DTime","Extrinsics time: %f\n",((float)(getRealTime() - startExtrinsics))*1000.0);

    // Necessary, because glMv is already transformed for OpenGL.
    double startInvRow = getRealTime();
    glMvTransposed.row(2) *= -1;
    glMvTransposed.row(2).copyTo(extrinsics.row(2));
    __android_log_print(ANDROID_LOG_DEBUG,"2D3DTime","InvRow time: %f\n",((float)(getRealTime() - startInvRow))*1000.0);

    double startMatMul = getRealTime();

    *points2d = (intrinsics*extrinsics)*points3d;
    __android_log_print(ANDROID_LOG_DEBUG,"2D3DTime","MatMul time: %f\n",((float)(getRealTime() - startMatMul))*1000.0);

    __android_log_print(ANDROID_LOG_DEBUG,"2D3DTime","2dTo3d time: %f\n",((float)(getRealTime() - start))*1000.0);
}

vector<Mat> hogDescriptorMats;
vector<vector<float> > hogDescriptors;

/**
* Load hog descriptors
*/
JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_generateHOGDescriptors(JNIEnv *env, jobject object, jstring renderImgsPath) {
	const char *renderImgsPathStr= env->GetStringUTFChars(renderImgsPath,0);
	char *s = new char[29];

#if DEBUG
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Paths: %s",renderImgsPathStr);
#endif

	HogDetector::loadRenderedImages(renderImgsPathStr);

	// HogDetector::findBestMatches();

	// int finalPhi = -1;
	// int finalTheta = -1;

	// int phiValues[] = {20,40};
	// int thetaValues[] = {0,10,20,30,40,50,60,70,90};
//
// 	vector<Mat> renderedImages;
//
// 	int i = 0;
// 	for (int *phi = phiValues; phi != phiValues+(sizeof(phiValues)/sizeof(int)); ++phi) {
//     	for (int *theta = thetaValues; theta != thetaValues+(sizeof(thetaValues)/sizeof(int)); ++theta, ++i) {
// 			sprintf(s, "image_%03d_p%03d_t%03d_r%03d.png", i, *phi, *theta, 2);
// 			string path = string(renderImgsPathStr) + string("/") + string(s);
// #if DEBUG
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Path: %s", path.c_str());
// #endif
// 			renderedImages.push_back(imread(path.c_str()));
// 		}
//     }
// 	imwrite("/sdcard/arbg/testImg.png", renderedImages[5]);
//
// 	Mat resized;
// 	Size winSize(128,128);
// 	Size cellSize(8,8);
// 	vector<float> descriptorValues;
// 	vector<Point> locations;
// 	Mat hogFeat;
//
// 	for(int i = 0; i < renderedImages.size(); i++)
// 	{
// 		resize(renderedImages[i],resized,winSize);
// 		cvtColor(resized, resized, CV_RGB2GRAY);
//
// 		calculateHOGDescriptor(resized, descriptorValues, winSize, cellSize, locations);
//
// #if DEBUG
// 		resize(renderedImages[i],resized,winSize);
// 		char *hogFileName = new char[24];
// 		sprintf(hogFileName,"/sdcard/arbg/hog%03d.png",i);
// 		imwrite(hogFileName,
// 			get_hogdescriptor_visual_image(resized, descriptorValues,
// 		                                   winSize,
// 		                                   cellSize,
// 		                                   5,1));
// 		delete[] hogFileName;
// #endif
//
// 		hogFeat = Mat(descriptorValues.size(),1,CV_32FC1);
//
// 		for(int i=0;i<descriptorValues.size();i++){
//   			hogFeat.at<float>(i,0)=descriptorValues.at(i);
// 		}
//
// 		hogDescriptorMats.push_back(hogFeat);
// 		hogDescriptors.push_back(descriptorValues);
// 	}
//
// 	delete[] s;
}


/**
* Find Lego Bricks Algorithm 3: Using CAD 3D models
*/
JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrick3(JNIEnv *env, jobject object, jlong bgrPointer, jlong resultMatPtr) {


	/**
	* TODO:
	*   - Test on Real input frame.
	*   - Brick 2D -> 3D + Rotation (Phi & Theta) => Final Result for 1 LegoBrick
	*/

	Mat *resultMat = (Mat *)resultMatPtr;

	// NEVER USE JPG!!
    // string testPath = dir + "/hogTestImg.png";
    // LOGD("InputImg: %s\n",testPath.c_str());
    // input = imread(testPath.c_str());

	vector<ResultingMatch> results = HogDetector::findBestMatches(*(Mat *)bgrPointer);


	/**
	*  - Real height of template = 36.138484212 mm
	*  - Focus lengte calibratie = 4.1mm
	*  - F_x = 3984.316735664
	*  - F_y = 3989.721360211
	*  - F_x = f * m_x }
	*  - F_y = f * m_y } => 3987.019047938 px / 4.1 mm = 973.102770783 px / mm
	*  		To lower Resolution (640x480) => 150.766460501 px / mm
	*
	*  - Distance to template plane = 36.138484212 mm * 4.1 mm / height of template in image
	*/

	int maxIdx = -1;
	double max = 0;

	for(int i = 0; i < results.size();i++) {
		if(results[i].score > max) {
			maxIdx = i;
			max = results[i].score;
		}
	}

	#define LOG_TAG "HOGDETECT"

	// float brickPixelHeight = (411.0-222.0)*(results[maxIdx].templateSize.height/600.0);

	float distanceToVpTemplateMm = (36.138484212 * 4.1) / (results[maxIdx].templateSize.height / 150.766460501);
	float distanceToBrickMm = distanceToVpTemplateMm + (results[maxIdx].rho * 25.4);

	LOGD("DistanceToBrick (mm): %f\n",distanceToBrickMm);

	(*resultMat) = Mat(1,7,CV_32FC1);

	// for(int i = 0; i < results.size();i++) {
		(*resultMat).at<float>(0,0) = distanceToBrickMm * (1.0/10.0);
		(*resultMat).at<float>(0,1) = results[maxIdx].phi;
		(*resultMat).at<float>(0,2) = results[maxIdx].theta;
		(*resultMat).at<float>(0,3) = results[maxIdx].location.x;
		(*resultMat).at<float>(0,4) = results[maxIdx].location.y;
		(*resultMat).at<float>(0,5) = results[maxIdx].templateSize.width;
		(*resultMat).at<float>(0,6) = results[maxIdx].templateSize.height;
	// }

// 	Mat bgr = *(Mat *)bgrPointer;
// 	Mat *brickPositions = (Mat *)brickPositionsPtr;
// 	const char *renderImgsPathStr= env->GetStringUTFChars(renderImgsPath,0);
// 	char *s = new char[29];
//
// #if DEBUG
// 	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Paths: %s",renderImgsPathStr);
// #endif
//
// 	int finalPhi = -1;
// 	int finalTheta = -1;
//
// 	int phiValues[] = {20,40};
// 	int thetaValues[] = {0,11,23,34,46,58,69,81,92};
//
// 	vector<Mat> renderedImages;
//
// 	int i = 0;
// 	for (int *phi = phiValues; phi != phiValues+(sizeof(phiValues)/sizeof(int)); ++phi) {
//     	for (int *theta = thetaValues; theta != thetaValues+(sizeof(thetaValues)/sizeof(int)); ++theta, ++i) {
// 			sprintf(s, "image_%03d_p%03d_t%03d_r%03d.png", i, *phi, *theta, 2);
// 			string path = string(renderImgsPathStr) + string("/") + string(s);
// #if DEBUG
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Path: %s", path.c_str());
// #endif
// 			renderedImages.push_back(imread(path.c_str()));
// 		}
//     }
// 	imwrite("/sdcard/arbg/testImg.png", renderedImages[5]);
//
// 	Mat resized;
// 	Size winSize(128,128);
// 	Size cellSize(8,8);
// 	// resize(renderedImages[5],resized,winSize);
// 	// cvtColor(resized, resized, CV_RGB2GRAY);
// 	//
// 	vector<float> descriptorValues;
// 	vector<Point> locations;
// 	//
// 	// calculateHOGDescriptor(resized, descriptorValues, winSize, cellSize, locations);
// 	//
// 	resize(renderedImages[5],resized,winSize);
//
// 	imwrite("/sdcard/arbg/hog.png",
// 		get_hogdescriptor_visual_image(resized, hogDescriptors[5],
// 	                                   winSize,
// 	                                   cellSize,
// 	                                   5,1));
//
// 	Mat subImg;
// 	Mat hogFeat;
// 	Mat output;
// 	Mat subImgGray;
// 	Mat tmp;
// 	Mat tmp2;
// 	Mat tmp2Gray;
// 	bgr.copyTo(output);
// 	vector<double> norms;
// 	for(int row = 0; row+winSize.height-1 < bgr.rows; row+=50) {
// 		for(int col = 0; col+winSize.width-1 < bgr.cols; col+=50) {
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"HOG Detector ROI from (%d,%d) to (%d,%d)",row,col,row+winSize.height-1, col+winSize.width-1);
// 			subImg = bgr(cv::Range(row, row+winSize.height), cv::Range(col, col+winSize.width));
// 			subImg.copyTo(tmp2);
// 			// imwrite("/sdcard/arbg/subImg.png",tmp2);
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"HOG Detector ROI size: (%d,%d)",tmp2.rows,tmp2.cols);
// 			cvtColor(tmp2, tmp2Gray, CV_RGB2GRAY);
// 			calculateHOGDescriptor(tmp2Gray, descriptorValues, winSize, cellSize, locations);
// 			hogFeat = Mat(descriptorValues.size(),1,CV_32FC1);
// 			for(int i=0;i<descriptorValues.size();i++){
// 	  			hogFeat.at<float>(i,0)=descriptorValues.at(i);
// 			}
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"HOG Detector Calculating norm ...");
// 			double n = norm(hogFeat,hogDescriptorMats[5]);
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"HOG Detector Resulting norm: %f",n);
// 			norms.push_back(n);
// 		}
// 	}
//
// 	double maxNorm;
//     maxNorm = *std::max_element(norms.begin(), norms.end());
//
// 	for(int row = 0; row+winSize.height-1 < bgr.rows; row+=50) {
// 		for(int col = 0; col+winSize.width-1 < bgr.cols; col+=50) {
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"HOG Detector ROI from (%d,%d) to (%d,%d)",row,col,row+50, col+50);
// 			tmp = output(cv::Range(row, row+50), cv::Range(col, col+50));
// 			int idx = col/50+(row/50)*(bgr.cols/50);
// 			double norm = norms[idx];
// 			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"HOG Detector Rescaled norm: %f @idx %d",255*(norm/maxNorm),idx);
// 			tmp.setTo(Scalar(255*(norm/maxNorm),255*(norm/maxNorm),255*(norm/maxNorm)));
// 			// output.at<Vec3b>(row,col)[0] = 255*(norm/maxNorm);
// 			// output.at<Vec3b>(row,col)[1] = 255*(norm/maxNorm);
// 			// output.at<Vec3b>(row,col)[2] = 255*(norm/maxNorm);
// 		}
// 	}
// 	imwrite("/sdcard/arbg/hogMatchResult.png",output);
//
// 	delete[] s;
// 	// imwrite("/sdcard/arbg/thresholded.png", *thresholded);
// 	// imwrite("/sdcard/arbg/bgr.png", bgr);
}

/**
* Find Lego Bricks Algorithm 4: Using LINE detection
*/
JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_findLegoBrickLines(JNIEnv *env, jobject object, jlong bgrPointer, jfloat upAngle, jlong resultMatPtr, jlong origContMatPtr) {
	Mat frame = *(Mat *)bgrPointer;
	Mat *result = (Mat *)resultMatPtr;
	Mat *origContMat = (Mat *)origContMatPtr;

	long start = getRealTime();
	BrickDetectorLines::TrackBricks(frame, upAngle, *result, *origContMat);
	LOGD("BrickDetection time (C++ part): %f\n",(float)((getRealTime()-start)*100.0f));
}

JNIEXPORT jfloatArray JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_checkOverlap(JNIEnv *env, jobject object, jlong inputPoints, jint idx) {
	Mat inputThresh = *(Mat *)inputPoints;
	jfloat overlapResult[2];

	BrickDetectorLines::CheckOverlap(inputThresh, idx, overlapResult[0], overlapResult[1]);

	jfloatArray javaResult;
 	javaResult = env->NewFloatArray(2);
 	if (javaResult == NULL) {
    	return NULL; /* out of memory error thrown */
 	}
 	env->SetFloatArrayRegion(javaResult, 0, 2, overlapResult);

	return javaResult;
}

JNIEXPORT jfloatArray JNICALL Java_be_wouterfranken_arboardgame_gameworld_LegoBrick_checkCurrentOverlap(JNIEnv *env, jobject object, jlong inputPoints) {
	Mat inputThresh = *(Mat *)inputPoints;
	jfloat overlapResult[1];

	BrickDetectorLines::CheckCurrFrameOverlap(inputThresh, overlapResult[0]);

	jfloatArray javaResult;
 	javaResult = env->NewFloatArray(1);
 	if (javaResult == NULL) {
    	return NULL; /* out of memory error thrown */
 	}
 	env->SetFloatArrayRegion(javaResult, 0, 1, overlapResult);

	return javaResult;
}

JNIEXPORT jfloatArray JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_LegoBrickTracker_getOverlap(JNIEnv *env, jobject object, jlong bricksPointer) {
	Mat bricksPoints = *(Mat *)bricksPointer;
	int amountBricks = bricksPoints.rows;
	jfloat overlapResult[amountBricks];

	#pragma omp parallel for num_threads(4)
	for (int i = 0; i < amountBricks; ++i)
	{

		LOGD("OPENMP threads: %d",omp_get_num_threads());

		vector<Point> points;
		for(int pidx = 0; pidx < 8; pidx++) {
			points.push_back(Point(bricksPoints.at<float>(i,pidx*2), bricksPoints.at<float>(i,pidx*2+1)));
		}
		// float tmp;
		Mat pts(points);
		float result;
		BrickDetectorLines::CheckCurrFrameOverlap(pts, result);

		#pragma omp critical
		{
			overlapResult[i] = result;
		}
	}

	jfloatArray javaResult;
 	javaResult = env->NewFloatArray(amountBricks);
 	if (javaResult == NULL) {
    	return NULL; /* out of memory error thrown */
 	}
 	env->SetFloatArrayRegion(javaResult, 0, amountBricks, overlapResult);

	return javaResult;
}


