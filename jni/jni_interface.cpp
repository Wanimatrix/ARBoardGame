/*
 * jni_interface.cpp
 *
 *  Created on: 5-okt.-2014
 *      Author: Wouter Franken
 */
// Libraries (C,OpenCV,Android,Aruco,OpenMP,...)
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

extern "C" {
	#include "getRealTime.c"
}

#define APPNAME "be.wouterfranken.arboardgame"
#define DEBUG 0
#define TIMING 1
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
	// YELLOW,
	// BLUE, // Add extra colors here + in the array below!


	COLOR_ITEM_AMOUNT};
struct HSVColorBounds {
	Scalar lower;
	Scalar upper;
	int close_kernel_size;
	int open_kernel_size;
};

struct HSVColorBounds hsvColors[COLOR_ITEM_AMOUNT] =
	{
		{Scalar(160,153,30),Scalar(180,255,255), 5, 0} // RED
		// {Scalar(135,147,30),Scalar(160,255,255), 9, 0}, // YELLOW
		// {Scalar(0,42,13),Scalar(112,255,255), 0, 17} // BLUE
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

	// imwrite("/sdcard/arbg/thresholded.png",thresholded[0]);
	// bitwise_or(redThresh,yellowThresh,thresholded);

#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Threshold time: %f\n",((float)(getRealTime() - start))*1000.0);

	start = getRealTime();
#endif
	// Mat threshInv;
	// for (int i = 0; i < thresholded.size(); ++i)
	// {
	// 	bitwise_not(thresholded[i],threshInv);
	// 	morphologyEx(threshInv, thresholded[i], MORPH_CLOSE, kernel);
	// 	bitwise_not(thresholded[i],thresholded[i]);
	// }
	// imwrite("/sdcard/arbg/closing.png",thresholded[0]);
#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Closing time: %f\n",((float)(getRealTime() - start))*1000.0);
#endif

	vector<Mat> response(thresholded.size());
	vector<vector<vector<Point> > > contours(thresholded.size());
	vector<vector<Vec4i> > hierarchy(thresholded.size());
	vector <KeyPoint> kpts;

#if WRITE_CONTOURS(true)
	Mat thresholdImgColor;//1, thresholdImgColor2, thresholdImgColor3;
	cvtColor(thresholded[RED],thresholdImgColor,COLOR_GRAY2BGR);
	// cvtColor(thresholded[RED],thresholdImgColor2,COLOR_GRAY2BGR);
	// cvtColor(thresholded[RED],thresholdImgColor3,COLOR_GRAY2BGR);
	// imwrite("/sdcard/arbg/orig.png",bgr);
#endif
#if TIMING
	start=getRealTime();
#endif
	/// Detect edges using canny
	for (int col = 0; col < thresholded.size(); ++col) Canny( thresholded[col], response[col], 100, 100*2, 3);
	// imwrite("/sdcard/arbg/canny.png",response[0]);
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
			// drawContours( thresholdImgColor2, contours[c], i, color, 2, 8, hierarchy[c], 0, Point() );
#endif
		}
	}
	// imwrite("/sdcard/arbg/beforePoly.png",thresholdImgColor1);
	// imwrite("/sdcard/arbg/afterPoly.png",thresholdImgColor2);
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
		int k = 0;
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
				Scalar color = Scalar(rand() % 256,rand() % 256,rand() % 256);
				drawContours( outImg, contours[col], k, color, 2, 8, hierarchy[col], 0, Point() );
	#endif

				i++;
				k++;
			}
		}
	}
	// imwrite("/sdcard/arbg/afterFiltering.png",thresholdImgColor3);
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

	morphology_operations(*thresholded, *thresholded);
	// bitwise_not(*thresholded,threshInv);
	// morphologyEx(threshInv, *thresholded, MORPH_CLOSE, kernel);
	// bitwise_not(*thresholded,*thresholded);

	#if TIMING
	__android_log_print(ANDROID_LOG_DEBUG,TAG,"Closing time: %f\n",((float)(getRealTime() - start))*1000.0);
	#endif

	// imwrite("/sdcard/arbg/thresholded.png", *thresholded);
	// imwrite("/sdcard/arbg/bgr.png", bgr);
}

void morphology_operations(Mat src, Mat dst) {
	bitwise_not(src,dst);
	// if(hsvColors[i].close_kernel_size > 0) {
		morphologyEx(dst, dst, MORPH_CLOSE, getStructuringElement(MORPH_ELLIPSE,
		Point(9,9)));
	// }
	// if(hsvColors[i].open_kernel_size > 0) {
		morphologyEx(dst, dst, MORPH_OPEN, getStructuringElement(MORPH_ELLIPSE,
		Point(17,17)));
	// }
	bitwise_not(dst,dst);
}
