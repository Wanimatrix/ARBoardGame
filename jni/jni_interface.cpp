/*
 * jni_interface.cpp
 *
 *  Created on: 5-okt.-2014
 *      Author: Wouter Franken
 */


#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/nonfree/features2d.hpp>
#include <opencv2/nonfree/nonfree.hpp>
#include "opencv2/calib3d/calib3d.hpp"
#include <android/log.h>
#include <iostream>
#include <string>
#include <limits.h>
#include <stdio.h>
#include "utilities.hpp"
#include "app.hpp"
#include <aruco.h>
#include <math.h>
#include <highlyreliablemarkers.h>

#define APPNAME "be.wouterfranken.arboardgame"

using namespace cv;

extern CalibrationData calibData;

extern "C"
{
	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_saveMat(
			JNIEnv * env, jclass javaClass, jstring filePath, jstring matName, jlong matPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_loadMatFromFile(
			JNIEnv * env, jclass javaClass, jstring filePath, jstring matName, jlong matPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_buildAndTrainPattern(
					JNIEnv * env, jobject jobject, jlong patternImgPtr, jstring cameraIntDistPath);

	JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_00024FindCameraPose_getCameraPose
	  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong pointsPtr, jlong cameraPosePtr);

	JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_getCameraPose2
	  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong pointsPtr, jlong cameraPosePtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_loadCameraCalibration
		  (JNIEnv * env, jobject jobject, jstring cameraIntDistPath);

	JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_getCameraPose3
	  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong projMatPtr, jlong mvMatPtr);

	JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_getCameraCalibration(JNIEnv * env, jobject jobject, jlong camMatPtr);
}

extern "C" void monstartup(char const*);

extern "C" void moncleanup();


JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_saveMat(
		JNIEnv * env, jclass javaClass, jstring filePath, jstring matName, jlong matPtr)
{
	Mat *mat = (Mat *) matPtr;

	const char * path = env->GetStringUTFChars(filePath , NULL );
	const char * matNm = env->GetStringUTFChars(matName , NULL );

	Utilities::saveMat(path, matNm, *mat);

	env->ReleaseStringUTFChars(filePath, path);
	env->ReleaseStringUTFChars(matName, matNm);
}

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_utilities_JNILib_loadMatFromFile(
		JNIEnv * env, jclass javaClass, jstring filePath, jstring matName, jlong matPtr)
{
	const char * path = env->GetStringUTFChars(filePath, NULL);
	const char * mName = env->GetStringUTFChars(matName, NULL);

	Mat *result = (Mat *) matPtr;

	Utilities::loadMatFromFile(path, mName, *result);

	env->ReleaseStringUTFChars(filePath, path);
	env->ReleaseStringUTFChars(matName, mName);
}

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_buildAndTrainPattern(
				JNIEnv * env, jobject jobject, jlong patternImgPtr, jstring cameraIntDistPath)
{
////	monstartup("libjni_interface.so");
//	Mat *patternImage = (Mat*) patternImgPtr;
//	const char * path = env->GetStringUTFChars(cameraIntDistPath, NULL);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Pattern building started...");
//
//	pd.buildPatternFromImage(*patternImage, p);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Pattern built...");
//
//	pd.train(p);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Pattern trained...");
//
//	Utilities::loadCameraCalibration(path, calibData.intrinsics, calibData.distortion);
//	env->ReleaseStringUTFChars(cameraIntDistPath, path);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Intrinsics: %d",calibData.intrinsics.cols*calibData.intrinsics.rows);
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Distortion size: %d",calibData.distortion.cols*calibData.distortion.rows);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Camera calibration done...");
}

JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_00024FindCameraPose_getCameraPose
  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong pointsPtr, jlong cameraPosePtr)
{
//	Mat *camPose = (Mat *) cameraPosePtr;
//	Mat *frameImage = (Mat *) frameImagePtr;
//	Mat *points = (Mat *) pointsPtr;
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Search for cameraPose started...");
//
//	bool patternFound = pd.findPattern(*frameImage, pti);
//
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Patterns found? %d",patternFound);
//
//	if(patternFound) {
//		pti.computePose(p, calibData.intrinsics, calibData.distortion);
//		*camPose = pti.pose3d;
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Pose computed");
//	}
//
//	*points = Mat(pti.points2d, true);
//
//	frameImage->release();
//
//	return false;
}

bool isRectangle(const std::vector<cv::Point>& contour, std::vector<cv::Point>& corners);

struct contour_sorter // 'less' for contours
{
    bool operator ()( const vector<Point>& a, const vector<Point> & b )
    {
//        Rect ra(boundingRect(a));
//        Rect rb(boundingRect(b));
        // scale factor for y should be larger than img.width
        return ( contourArea(a) < contourArea(b) );
    }
};

bool acceptLinePair(Vec2f line1, Vec2f line2, float minTheta)
{
    float theta1 = line1[1], theta2 = line2[1];

    if(theta1 < minTheta)
    {
        theta1 += CV_PI; // dealing with 0 and 180 ambiguities...
    }

    if(theta2 < minTheta)
    {
        theta2 += CV_PI; // dealing with 0 and 180 ambiguities...
    }

    return abs(theta1 - theta2) > minTheta;
}

vector<Point2f> lineToPointPair(Vec2f line)
{
    vector<Point2f> points;

    float r = line[0], t = line[1];
    double cos_t = cos(t), sin_t = sin(t);
    double x0 = r*cos_t, y0 = r*sin_t;
    double alpha = 1000;

    points.push_back(Point2f(x0 + alpha*(-sin_t), y0 + alpha*cos_t));
    points.push_back(Point2f(x0 - alpha*(-sin_t), y0 - alpha*cos_t));

    return points;
}

// the long nasty wikipedia line-intersection equation...bleh...
Point2f computeIntersect(Vec2f line1, Vec2f line2)
{
    vector<Point2f> p1 = lineToPointPair(line1);
    vector<Point2f> p2 = lineToPointPair(line2);

    float denom = (p1[0].x - p1[1].x)*(p2[0].y - p2[1].y) - (p1[0].y - p1[1].y)*(p2[0].x - p2[1].x);
    Point2f intersect(((p1[0].x*p1[1].y - p1[0].y*p1[1].x)*(p2[0].x - p2[1].x) -
                       (p1[0].x - p1[1].x)*(p2[0].x*p2[1].y - p2[0].y*p2[1].x)) / denom,
                      ((p1[0].x*p1[1].y - p1[0].y*p1[1].x)*(p2[0].y - p2[1].y) -
                       (p1[0].y - p1[1].y)*(p2[0].x*p2[1].y - p2[0].y*p2[1].x)) / denom);

    return intersect;
}

JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_getCameraPose2
  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong pointsPtr, jlong cameraPosePtr)
{
	Mat *camPose = (Mat *) cameraPosePtr;
	Mat *frameImage = (Mat *) frameImagePtr;
	Mat *points = (Mat *) pointsPtr;

	Mat binFrameImg;
	Mat edges;
	cv::threshold(*frameImage, binFrameImg, 100, 255, cv::THRESH_BINARY);
	GaussianBlur(binFrameImg, binFrameImg, Size(7, 7), 2.0, 2.0);
	Canny(binFrameImg, edges, 66.0, 133.0, 3);
	vector<Vec2f> lines;
	HoughLines( edges, lines, 1, CV_PI/180, 50, 0, 0 );
	Mat drawing = Mat::zeros( binFrameImg.size(), CV_8UC3 );
	cvtColor(*frameImage, drawing, CV_GRAY2RGB);
	vector<Point2f> intersections;

	for( size_t i = 0; i < lines.size(); i++ )
	{
		for(size_t j = 0; j < lines.size(); j++)
		{
			Vec2f line1 = lines[i];
			Vec2f line2 = lines[j];
			if(acceptLinePair(line1, line2, CV_PI / 32))
			{
				Point2f intersection = computeIntersect(line1, line2);
				intersections.push_back(intersection);
			}
		}
		vector<Point2f> pointPair = lineToPointPair(lines[i]);

		line(drawing,pointPair[0],pointPair[1],Scalar(255,0,0),2,8);
	}

//	vector<vector<Point2f> > contours;
//	for (int i = 0; i < intersections.size(); ++i) {
//		for (int j = 0; i < intersections.size(); ++i) {
//			for (int k = 0; i < intersections.size(); ++i) {
//				for (int l = 0; i < intersections.size(); ++i) {
//					contours.
//				}
//			}
//		}
//	}
//	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Amount intersections: %d",intersections.size());


	if(intersections.size() > 0)
	{
		vector<Point2f>::iterator i;
		for(i = intersections.begin(); i != intersections.end(); ++i)
		{
			std::cout << "Intersection is " << i->x << ", " << i->y << std::endl;
			circle(drawing, *i, 1, Scalar(0, 255, 0), 3);
		}
	}

	cv::imwrite("/sdcard/nonfree/contours.png", drawing);

//	cv::Canny(*frameImage, binFrameImg, 100, 100, 3);
//	vector<vector<cv::Point> > contours;
//	vector<cv::Vec4i> hierarchy;
//	cv::findContours(binFrameImg, contours, hierarchy, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE);

//	std::sort(contours.begin(),contours.end(),contour_sorter());
//
//	contours.
//
//	Mat drawing = Mat::zeros( binFrameImg.size(), CV_8UC3 );
//	RNG rng(12345);
//	Rect rect1;
//	Rect rect2;
//	float epsilon = 0.3f;
//	for( int i = 0; i< contours.size(); i++ )
//	{
//		for( int j = i+1; j< contours.size(); j++ ) {
//
//			rect1 = boundingRect(contours[i]);
//			rect2 = boundingRect(contours[j]);
//
//			vector<Point> c1;
//			vector<Point> c2;
//
//			float div = rect1.area() < rect2.area() ? ((float)rect2.area())/rect1.area() : ((float)rect1.area())/rect2.area();
//
//			if(div < 3.2f+epsilon && div > 3.2f-epsilon) {
//				if(isRectangle(contours[i], c1) && isRectangle(contours[j], c2)) {
//
//#if _DEBUG
//					Scalar color = Scalar( rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );
//					drawContours( drawing, contours, i, color, 1, 8, hierarchy, 0, Point() );
//					color = Scalar( rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) );
//					drawContours( drawing, contours, j, color, 1, 8, hierarchy, 0, Point() );
//#endif
//
//
//
//					if(fabs(contourArea(Mat(c1))) > fabs(contourArea(Mat(c2)))) {
//						*points = cv::Mat(c1 ,true);
//
//#if _DEBUG
//						for (int k = 0; k < c1.size(); ++k) {
//							circle(drawing,
//									c1.at(k),
//									3,
//									Scalar( rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) ),
//									8);
//						}
//#endif
//					}
//					else {
//						*points = cv::Mat(c2, true);
//
//						for (int k = 0; k < c2.size(); ++k) {
//#if _DEBUG
//							circle(drawing,
//									c2.at(k),
//									3,
//									Scalar( rng.uniform(0, 255), rng.uniform(0,255), rng.uniform(0,255) ),
//									8);
//#endif
//						}
//					}
//				}
//			}
//
//		}
//	}
//
//#if _DEBUG
//	cv::imwrite("/sdcard/nonfree/contours.png", drawing);
//#endif

	return true;
}

static double angle( Point pt1, Point pt2, Point pt0 )
{
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;
    return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
}

bool isRectangle(const std::vector<cv::Point>& contour, std::vector<cv::Point>& corners) {
	approxPolyDP(Mat(contour), corners, arcLength(Mat(contour), true)*0.02, true);

	if (corners.size() == 4 &&
			fabs(contourArea(Mat(corners))) > 1000 &&
			isContourConvex(Mat(corners)))
	{
		double maxCosine = 0;

		for( int j = 2; j < 5; j++ )
		{

			double cosine = fabs(angle(corners[j%4], corners[j-2], corners[j-1]));
			maxCosine = MAX(maxCosine, cosine);
		}

		if( maxCosine < 0.3 )
			return true;
		else
			return false;
	}
	return false;
}

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_loadCameraCalibration(
				JNIEnv * env, jobject jobject, jstring cameraIntDistPath)
{
	const char * path = env->GetStringUTFChars(cameraIntDistPath, NULL);

	Utilities::loadCameraCalibration(path, calibData.intrinsics, calibData.distortion);
//	camParams.readFromXMLFile(path);
	env->ReleaseStringUTFChars(cameraIntDistPath, path);

	__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Intrinsics: %d",calibData.intrinsics.cols*calibData.intrinsics.rows);
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Distortion size: %d",calibData.distortion.cols*calibData.distortion.rows);

	__android_log_print(ANDROID_LOG_DEBUG,APPNAME, "Camera calibration done...");
}

void printPoint3f(const Point3f& p) {
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"(%f,%f,%f)",p.x,p.y,p.z);
}
void printPoint2f(const Point2f& p) {
	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"(%f,%f)",p.x,p.y);
}

double glProjMatrix[16];
double glModelViewMatrix[16];

aruco::Marker m;

JNIEXPORT jboolean JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_getCameraPose3
  	  (JNIEnv * env, jobject jobject, jlong frameImagePtr, jlong projMatPtr, jlong mvMatPtr) {

	Mat *mv = (Mat *) mvMatPtr;
	Mat *proj = (Mat *) projMatPtr;
	Mat *frameImage = (Mat *) frameImagePtr;
//	Mat *points = (Mat *) pointsPtr;

//	std::clock_t start = std::clock();
	aruco::MarkerDetector detector;
	float markerSize = 6.4f;
//	camParams.resize(frameImage->size());
	detector.pyrDown(1);
//	detector.setDesiredSpeed(2);
	std::vector<aruco::Marker> markers;
//	Size calibImgSize(2592,1944);
//	Size calibImgSize(1280,720);
	Size inputImgSize = frameImage->size();
	aruco::CameraParameters camParams(calibData.intrinsics,calibData.distortion,calibData.imgSize);
	camParams.resize(inputImgSize);
	detector.detect(*frameImage,markers,camParams.CameraMatrix,Mat(),markerSize);//intrinsics,distortionCoeff,markerSize);



	cv::Mat Rvec;
	cv::Mat_<float> Tvec;
	cv::Mat raux,taux;

	if(markers.size() > 0) {
		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Marker detected");

//		Mat drawing;
//		cvtColor(*frameImage, drawing, CV_GRAY2RGB);

//		markers[0].draw(drawing,Scalar(0,0,255),2);


//		cv::imwrite("/sdcard/nonfree/marker.png",drawing);

		std::vector<cv::Point2f> pointVec;
//		pointVec.push_back(markers[0][0]);
//		pointVec.push_back(markers[0][1]);
//		pointVec.push_back(markers[0][3]);
//		pointVec.push_back(markers[0][2]);
//
//		m = markers[0];

//		*points = Mat(pointVec,true);


		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"(%f,%f)",markers[0][1].x,markers[0][1].y);

		double arucoProj[16];
		double arucoMv[16];

//		[2*K00/width, -2*K01/width,    (width - 2*K02 + 2*x0)/width,                            0]
//		[          0, 2*K11/height, (-height + 2*K12 + 2*y0)/height,                            0]
//		[          0,            0,  (-zfar - znear)/(zfar - znear), -2*zfar*znear/(zfar - znear)]
//		[          0,            0,                              -1,                            0]

//		double ownProj[16];
//		ownProj[0] = 2*camParams.CameraMatrix.at<double>(0,0)*640/inputImgSize.width;
//		ownProj[1] = -2*camParams.CameraMatrix.at<double>(0,1)*640/inputImgSize.width;
//		ownProj[2] = (inputImgSize.width-2*camParams.CameraMatrix.at<double>(0,2)*640)/inputImgSize.width;
//		ownProj[5] = -2*camParams.CameraMatrix.at<double>(1,1)*480/inputImgSize.height;
//		ownProj[6] = (inputImgSize.height-2*camParams.CameraMatrix.at<double>(1,2)*480)/inputImgSize.height;
//		ownProj[10] = (-100 - 0.1)/(100 - 0.1);
//		ownProj[11] = -2*100*0.1/(100 - 0.1);
//		ownProj[14] = -1;

		camParams.glGetProjectionMatrix(inputImgSize,/*Size(renderImgWidth,renderImgHeight)*//*Size((int)(640.0f/480),1)*/Size(640,480),arucoProj,10,10000,false);
//		markers[0].calculateExtrinsics(markerSize,camParams,false);
		markers[0].glGetModelViewMatrix(arucoMv);

		Mat arucoProjMat = Mat::zeros(4,4,CV_64F);
		Mat arucoMvMat = Mat::zeros(4,4,CV_64F);
		for (int i = 0; i < 16; ++i) {
				arucoProjMat.at<double>(i/4,i%4) = arucoProj[i];
				arucoMvMat.at<double>(i/4,i%4) = arucoMv[i];
		}

//		Utilities::logMat(arucoProjMat,"Aruco Proj Matrix");
//		Utilities::logMat(arucoMvMat,"Aruco MV Matrix");
//		Utilities::logMat(calibData.intrinsics,"Intrinsics");
//		Utilities::logMat(calibData.distortion,"Distortion");
//
		*proj = arucoProjMat;
		*mv = arucoMvMat;
		return true;

//		Size size = calibData.imgSize;
//		Point2f focalLength(calibData.intrinsics.at<double>(0,0),calibData.intrinsics.at<double>(1,1));
//		Point2f principalPoint(calibData.intrinsics.at<double>(0,2),calibData.intrinsics.at<double>(1,2));
//		float farPlane = 100;
//		float nearPlane = 0.1;
//		float dx = principalPoint.x - size.width / 2;
//		float dy = principalPoint.y - size.height / 2;
//		float x =  2.0f * focalLength.x / size.width;
//		float y = -2.0f * focalLength.y / size.height;
//		float a =  2.0f * dx / size.width;
//		float b = -2.0f * (dy + 1.0f) / size.height;
//		float c = (farPlane + nearPlane) / (farPlane - nearPlane);
//		float d = -nearPlane * (1.0f + c);

//		mat.data[0] = x;      mat.data[1] = 0.0f;   mat.data[2] = 0.0f;  mat.data[3] = 0.0f;
//		mat.data[4] = 0.0f;   mat.data[5] = y;      mat.data[6] = 0.0f;  mat.data[7] = 0.0f;
//		mat.data[8] = a;      mat.data[9] = b;      mat.data[10] = c;    mat.data[11] = 1.0f;
//		mat.data[12] = 0.0f;  mat.data[13] = 0.0f;  mat.data[14] = d;    mat.data[15] = 0.0f;

//		glProjMat.at<double>(0,0) = x;
//		glProjMat.at<double>(1,1) = y;
//		glProjMat.at<double>(2,0) = a;
//		glProjMat.at<double>(2,1) = b;
//		glProjMat.at<double>(2,2) = c;
//		glProjMat.at<double>(2,3) = 1;
//		glProjMat.at<double>(3,3) = d;
//
//		float fovXRadians = 2 * atan(0.5f * size.width / focalLength.x);
//		float fovYRadians = 2 * atan(0.5f * size.height / focalLength.y);
//		float fovXDegrees = fovXRadians * 180.0f / M_PI;
//		float fovYDegrees = fovYRadians * 180.0f / M_PI;

//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Fov (x,y): (%f,%f)",fovXDegrees,fovYDegrees);

//		*camPose = arucoMvMat * arucoProjMat;

		Mat persp = Mat::zeros(Size(4,4),CV_32F);
		persp.at<float>(0,0) = calibData.intrinsics.at<double>(0,0);
		persp.at<float>(1,1) = calibData.intrinsics.at<double>(1,1);
		persp.at<float>(2,0) = -calibData.intrinsics.at<double>(0,2);
		persp.at<float>(2,1) = -calibData.intrinsics.at<double>(1,2);
//		persp.at<float>(0,0) = 0.01f;
//		persp.at<float>(1,1) = 0.01f;
//		persp.at<float>(2,0) = 0;
//		persp.at<float>(2,1) = 0;
		persp.at<float>(2,2) = 10.0f+10000;
		persp.at<float>(3,2) = 10.0f*10000;
		persp.at<float>(2,3) = -1;

		Mat ndc = Mat::eye(Size(4,4),CV_32F);
		float left = 0;
		float right = calibData.imgSize.width;
		float top = 0;
		float bottom = calibData.imgSize.height;
		float far = 10000;
		float near = 10.0f;
		ndc.at<float>(0,0) = 2.0/(right-left);
		ndc.at<float>(1,1) = 2.0/(top-bottom);
		ndc.at<float>(2,2) = -2.0/(far-near);
		float tx = -(right+left)/(right-left);
		float ty = -(top+bottom)/(top-bottom);
		float tz = -(far+near)/(far-near);
		ndc.at<float>(3,0) = tx;
		ndc.at<float>(3,1) = ty;
		ndc.at<float>(3,2) = tz;

		Mat ndc2 = Mat::eye(Size(4,4),CV_32F);
		left = -1;
		right = 1;
		top = -1;
		bottom = 1;
		far = 1;
		near = -1;
		ndc2.at<float>(0,0) = 2.0/(right-left);
		ndc2.at<float>(1,1) = 2.0/(top-bottom);
		ndc2.at<float>(2,2) = -2.0/(far-near);
		tx = -(right+left)/(right-left);
		ty = -(top+bottom)/(top-bottom);
		tz = -(far+near)/(far-near);
		ndc2.at<float>(3,0) = tx;
		ndc2.at<float>(3,1) = ty;
		ndc2.at<float>(3,2) = tz;

		Mat persp2 = Mat::zeros(Size(4,4),CV_32F);
		persp2.at<float>(0,0) = near;
		persp2.at<float>(1,1) = near;
		persp2.at<float>(2,2) = near+far;
		persp2.at<float>(3,2) = near*far;
		persp2.at<float>(2,3) = -1;

//		Utilities::logMat(persp,"Perspective");
//		Utilities::logMat(ndc,"NDC");

		*proj = (persp*ndc);

		persp = Mat::zeros(Size(4,4),CV_32F);
		persp.at<float>(0,0) = near;
		persp.at<float>(1,1) = near;
//		persp.at<float>(0,0) = 0.01f;
//		persp.at<float>(1,1) = 0.01f;
//		persp.at<float>(2,0) = 0;
//		persp.at<float>(2,1) = 0;
		persp.at<float>(2,2) = 10.0f+1000;
		persp.at<float>(3,2) = 10.0f*1000;
		persp.at<float>(2,3) = -1;

		ndc = Mat::eye(Size(4,4),CV_32F);
		left = std::tan(38.341793*(3.141592/180.0)) * near;
		right = -std::tan(31.633959*(3.141592/180.0)) * near;
		top = -std::tan(41.097733*(3.141592/180.0)) * near;
		bottom = std::tan(38.169544*(3.141592/180.0)) * near;
		ndc.at<float>(0,0) = 2.0/(right-left);
		ndc.at<float>(1,1) = 2.0/(top-bottom);
		ndc.at<float>(2,2) = -2.0/(far-near);
		tx = -(right+left)/(right-left);
		ty = -(top+bottom)/(top-bottom);
		tz = -(far+near)/(far-near);
		ndc.at<float>(3,0) = tx;
		ndc.at<float>(3,1) = ty;
		ndc.at<float>(3,2) = tz;

		Mat mv2 = Mat::eye(Size(4,4),CV_32F);
		mv2.at<float>(3,2) = -3.4f;
		mv2.at<float>(1,1) = 480.0f/640.0f;

		Mat t = persp*ndc;
		Utilities::logMat(t,"Perspective");
		Utilities::logMat(mv2,"mv");

		Mat projTransp;
		Mat mvTransp;
		Mat perspTransp;
		transpose(*proj,projTransp);
		transpose(mv2,mvTransp);
		transpose(persp*ndc,perspTransp);

		*proj = perspTransp*mvTransp*projTransp;

//		10-22 23:07:59.714: D/VirtualLayerRenderer(28167): FOVs: 38.341793,31.633959,41.097733,38.169544;

		Utilities::logMat(*proj,"Projection");

		double Ax=double(inputImgSize.width)/double(calibData.imgSize.width);
		double Ay=double(inputImgSize.height)/double(calibData.imgSize.height);
//		double Ax=1.0/double(calibData.imgSize.width);
//		double Ay=1.0/double(calibData.imgSize.height);

		double fx = calibData.intrinsics.at<double>(0,0);
		double fy = calibData.intrinsics.at<double>(1,1);
		double cx = calibData.intrinsics.at<double>(0,2);
		double cy = calibData.intrinsics.at<double>(1,2);

		double _fx=fx*Ax;
		double _cx=cx*Ax;
		double _fy=fy*Ay;
		double _cy=cy*Ay;

		cv::Mat scaledCam = Mat::eye(3,3,CV_32F);
		scaledCam.at<float>(0,0) = _fx;
		scaledCam.at<float>(0,2) = _cx;
		scaledCam.at<float>(1,1) = _fy;
		scaledCam.at<float>(1,2) = _cy;

		double halfSize=markerSize/2.;

		std::vector<cv::Point3f> objPts(4);
		objPts[0] = Point3f(-halfSize,halfSize,0);
		objPts[1] = Point3f(halfSize,halfSize,0);
		objPts[2] = Point3f(-halfSize,-halfSize,0);
		objPts[3] = Point3f(halfSize,-halfSize,0);

		std::vector<cv::Point2f> markerPts(4);
		markerPts[0] = Point2f(markers[0][0].x,markers[0][0].y);
		markerPts[1] = Point2f(markers[0][1].x,markers[0][1].y);
		markerPts[2] = Point2f(markers[0][3].x,markers[0][3].y);
		markerPts[3] = Point2f(markers[0][2].x,markers[0][2].y);

//		Utilities::logMat(Mat(objPts),"Points3D");
//		Utilities::logMat(Mat(markerPts),"Points2D");

		cv::Mat raux,taux;
		cv::solvePnP(objPts, markerPts, scaledCam, calibData.distortion,raux,taux);
		raux.convertTo(Rvec,CV_32F);
		taux.convertTo(Tvec ,CV_32F);

		Mat Rot(3,3,CV_32FC1);
		cv::Rodrigues(Rvec, Rot);

		Mat modelview_matrix = Mat::zeros(Size(4,4),CV_32F);
		modelview_matrix.at<float>(0,0) = Rot.at<float>(0,0);
		modelview_matrix.at<float>(1,0) = Rot.at<float>(0,1);
		modelview_matrix.at<float>(2,0) = Rot.at<float>(0,2);
		modelview_matrix.at<float>(3,0) = Tvec.at<float>(0,0);
		modelview_matrix.at<float>(0,1) = Rot.at<float>(1,0);
		modelview_matrix.at<float>(1,1) = Rot.at<float>(1,1);
		modelview_matrix.at<float>(2,1) = Rot.at<float>(1,2);
		modelview_matrix.at<float>(3,1) = Tvec.at<float>(1,0);
		modelview_matrix.at<float>(0,2) = -Rot.at<float>(2,0);
		modelview_matrix.at<float>(1,2) = -Rot.at<float>(2,1);
		modelview_matrix.at<float>(2,2) = -Rot.at<float>(2,2);
		modelview_matrix.at<float>(3,2) = -Tvec.at<float>(2,0);
		modelview_matrix.at<float>(3,3) = 1.0;

		Mat drawing;
		cvtColor(*frameImage, drawing, CV_GRAY2RGB);
		aruco::CvDrawingUtils::draw3dCube(drawing, markers[0],camParams);
		markers[0].draw(drawing,Scalar(0,0,255),2);

		cv::imwrite("/sdcard/nonfree/cubeTest.png",drawing);

//		Mat testVector(Size(4,1),CV_32F);
//		testVector.at<float>(0) = 0.032f;
//		testVector.at<float>(1) = 0.032f;
//		testVector.at<float>(2) = 0;
//		testVector.at<float>(2) = 1;
//		Mat modelview_matrix_inv = Mat::zeros(Size(4,4),CV_32F);
//		cv::invert(modelview_matrix,modelview_matrix_inv);
//		Mat resultVector(Size(4,4),CV_32F);

//		resultVector = testVector*modelview_matrix;
//		Utilities::logMat(resultVector,"Result");

		Utilities::logMat(modelview_matrix,"ModelView");

		*mv = modelview_matrix;

//		return true;

//		Utilities::logMat(intrinsics,"Camera Matrix");
//		Utilities::logMat(distortionCoeff,"Camera distortion");
//
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Input Img size: (%d,%d)",inputImgSize.width,inputImgSize.height);
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Calib Img size: (%d,%d)",calibImgSize.width,calibImgSize.height);
//
//		double Ax=double(inputImgSize.width)/double(calibData.imgSize.width);
//		double Ay=double(inputImgSize.height)/double(calibData.imgSize.height);
//		double Ax=double(inputImgSize.width)/double(calibData.imgSize.width);
//		double Ay=double(inputImgSize.height)/double(calibData.imgSize.height);
//
//		double fx = calibData.intrinsics.at<double>(0,0);
//		double fy = calibData.intrinsics.at<double>(1,1);
//		double cx = calibData.intrinsics.at<double>(0,2);
//		double cy = calibData.intrinsics.at<double>(1,2);
//
//		double _fx=fx*Ax;
//		double _cx=cx*Ax;
//		double _fy=fy*Ay;
//		double _cy=cy*Ay;
//
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"InputImg Size: (%d,%d)",inputImgSize.width,inputImgSize.height);
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"CalibratedImg Size: (%d,%d)",calibData.imgSize.width,calibData.imgSize.height);
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Original (fx,fy,cx,cy) = (%f,%f,%f,%f)",fx,fy,cx,cy);
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Scaled (fx,fy,cx,cy) = (%f,%f,%f,%f)",_fx,_fy,_cx,_cy);
//
//////		double cparam[3][4] =
//////		{
//////			{_fx,  0,  _cx},
//////			{0,          _fy,  _cy},
//////			{0,      0,      1}
//////		};
//
//		pointVec.clear();
//		pointVec.push_back(markers[0][0]);
//		pointVec.push_back(markers[0][1]);
//		pointVec.push_back(markers[0][3]);
//		pointVec.push_back(markers[0][2]);
//
//
//
//
////
//		cv::Mat scaledCam = Mat::eye(3,3,CV_64F);
//		scaledCam.at<double>(0,0) = _fx;
//		scaledCam.at<double>(0,2) = _cx;
//		scaledCam.at<double>(1,1) = _fy;
//		scaledCam.at<double>(1,2) = _cy;
////
////		Utilities::logMat(scaledCam,"Scaled Camera Matrix");
////
////		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Scaled Cam Parameters calculated ...");
////
//		std::vector<cv::Point3f> points3d(4);
//		points3d[0] = cv::Point3f(-0.032,  0.032, 0);
//		points3d[1] = cv::Point3f( 0.032,  0.032, 0);
//		points3d[2] = cv::Point3f(-0.032, -0.032, 0);
//		points3d[3] = cv::Point3f( 0.032, -0.032, 0);
//
//
//		for (int i = 0; i < pointVec.size(); ++i) {
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Pt2D: (%f,%f)",pointVec[i].x,pointVec[i].y);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Pt3D: (%f,%f)",points3d[i].x,points3d[i].y);
//		}
////		// Check if objPoints and pointvec are corresponding! CORRECT!
////
////
////		std::for_each(points3d.begin(),points3d.end(),printPoint3f);
////
////		std::for_each(pointVec.begin(),pointVec.end(),printPoint2f);
////
////		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Point vectors initialized ...");
////
//		cv::solvePnP(points3d, pointVec, calibData.intrinsics, calibData.distortion, raux, taux,false,CV_P3P);
////
////		Utilities::logMat(raux,"Raux");
////		Utilities::logMat(taux,"Taux");
////
////		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"solvepnp done ...");
////
//		raux.convertTo(Rvec,CV_64F);
//		taux.convertTo(Tvec ,CV_64F);
////
//		cv::Mat_<double> rotMat(3,3);
//		cv::Rodrigues(Rvec, rotMat);
////
//		cv::Mat transformation = cv::Mat::eye(4,4,CV_64F);
//		for (int row = 0; row < 3; ++row) {
//			for (int col = 0; col < 3; ++col) {
//				transformation.at<double>(row,col) = rotMat.at<double>(row,col);
//			}
//			transformation.at<double>(row,3) = Tvec.at<double>(row,0);
//		}
//		transformation.at<double>(3,3) = 1.0;
////
//		Utilities::logMat(transformation,"Transformation");
//
//		Mat p = Mat::zeros(Size(1,4),CV_64F);
//		p.at<double>(0,0) = points3d[0].x;
//		p.at<double>(0,1) = points3d[0].y;
//		p.at<double>(0,2) = points3d[0].z;
//		p.at<double>(0,3) = 1;
//
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Lengths: %d %d",transformation.size().width,p.size().height);
//		Mat transposedTransf = cv::Mat::zeros(4,4,CV_64F);
//		Mat transposedProj;
//		Mat transposedMv;
//		cv::transpose(arucoProjMat,transposedProj);
//		cv::transpose(arucoMvMat,transposedMv);
//		Mat r = transposedProj * transposedMv * p;
//		r = calibData.intrinsics * r;
//		r = (r*inputImgSize.width)/r.at<double>(0,3);
//
//		Utilities::logMat(transposedTransf, "Inverted Transpose");
//
//		Utilities::logMat(Mat(points3d[0]),"Point3D");
//		Utilities::logMat(p,"Point3D");
//		Utilities::logMat(Mat(pointVec[0]),"Point");
//		Utilities::logMat(r,"Result");
//
////		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Transformation check: (%f,%f) == (%f,%f)",p.at<float>(0,0),p.at<float>(0,1),r.at<float>(0,0),r.at<float>(0,1));
//
//		return true;
//
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Transformation Matrix calculated ...");
//
//		cv::Mat scaleM = cv::Mat::eye(4,4,CV_32F);
//		scaleM.at<float>(1,1) = -1;
//		scaleM.at<float>(2,2) = -1;
//
//		cv::Mat modelView = scaleM * transformation;
//
//		cv::Mat transpModelView;
//		cv::transpose(modelView,transpModelView);
//
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"ModelViewMatrix calculated ...");
//
//		Utilities::logMat(modelView, "ModelView");
//
//		cv::Mat landscapeRotation = cv::Mat::eye(4,4,CV_32F);
//		landscapeRotation.at<float>(0,0) = 0;
//		landscapeRotation.at<float>(1,1) = 0;
//		landscapeRotation.at<float>(0,1) = 1;
//		landscapeRotation.at<float>(1,0) = -1;
//
//		int far = 10000;
//		int near = 0.1;
//		cv::Mat tmp = cv::Mat::zeros(4,4,CV_32F);
//		tmp.at<float>(0,0) = (2*scaledCam.at<float>(0,0))/inputImgSize.width;
//		tmp.at<float>(1,1) = 2*scaledCam.at<float>(1,1)/inputImgSize.height;
//		tmp.at<float>(0,2) = -1+(2*scaledCam.at<float>(0,2)/inputImgSize.width);
//		tmp.at<float>(1,2) = -1+(2*scaledCam.at<float>(1,2)/inputImgSize.height);
//		tmp.at<float>(2,2) = -(far+near)/(far-near);
//		tmp.at<float>(2,3) = -2*far*near/(far-near);
//		tmp.at<float>(3,2) = -1;
//
//		cv::Mat projection = /*landscapeRotation **/ tmp;
//
//		cv::Mat transpProj;
//		cv::transpose(projection,transpProj);
//
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"ProjectionMatrix calculated ...");
//
//		Utilities::logMat(projection, "Projection");
//
//		*camPose = transpModelView * arucoMvMat;
////		*camPose = transpModelView /** transpProj*/;
//
//		Utilities::logMat(*camPose,"Camera Pose");

//		camParams.glGetProjectionMatrix(Size(2592,1944),frameImage->size(),glProjMatrix,5.0,10000.0,true);
//		markers[0].calculateExtrinsics(0.064f,camParams, false);
//		markers[0].glGetModelViewMatrix(glModelViewMatrix);

//		jsize size = env->GetArrayLength(mv);
//		std::vector<double> input( size );
//		env->GetDoubleArrayRegion( arr, 0, size, &input[0] );

//		*mv = cv::Mat::zeros(Size(16, 0), CV_64F);
//		*prj = cv::Mat::zeros(Size(16, 0), CV_64F);
//
//		for (int i = 0; i < 16; ++i) {
//			mv->at<double>(i,0) = glModelViewMatrix[i];
//			prj->at<double>(i,0) = glProjMatrix[i];
//		}

//		mv = env->NewDoubleArray( 16);
//		env->SetDoubleArrayRegion(mv, 0, 16, &glModelViewMatrix[0]);
//		prj = env->NewDoubleArray( 16);
//		env->SetDoubleArrayRegion(prj, 0, 16, &glProjMatrix[0]);

//		if(glModelViewMatrix != NULL) {
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"MVP: %f,%f,%f,%f",glModelViewMatrix[0],glModelViewMatrix[1],glModelViewMatrix[2],glModelViewMatrix[3]);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"     %f,%f,%f,%f",glModelViewMatrix[4],glModelViewMatrix[5],glModelViewMatrix[6],glModelViewMatrix[7]);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"     %f,%f,%f,%f",glModelViewMatrix[8],glModelViewMatrix[9],glModelViewMatrix[10],glModelViewMatrix[11]);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"     %f,%f,%f,%f",glModelViewMatrix[12],glModelViewMatrix[13],glModelViewMatrix[14],glModelViewMatrix[15]);
//		}
//
//		if(glProjMatrix != NULL) {
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"MVP: %f,%f,%f,%f",glProjMatrix[0],glProjMatrix[1],glProjMatrix[2],glProjMatrix[3]);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"     %f,%f,%f,%f",glProjMatrix[4],glProjMatrix[5],glProjMatrix[6],glProjMatrix[7]);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"     %f,%f,%f,%f",glProjMatrix[8],glProjMatrix[9],glProjMatrix[10],glProjMatrix[11]);
//			__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"     %f,%f,%f,%f",glProjMatrix[12],glProjMatrix[13],glProjMatrix[14],glProjMatrix[15]);
//		}



//		std::stringstream buf;
//		buf << "Intrinsics: " << intrinsics;
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Intrinsics: %d",intrinsics.cols*intrinsics.rows);
//		__android_log_print(ANDROID_LOG_DEBUG,APPNAME "CAMPOSE","Distortion size: %d",distortionCoeff.cols*distortionCoeff.rows);

//		cv::solvePnP(points3d, pointVec, intrinsics, distortionCoeff,raux,taux);
//		raux.convertTo(Rvec,CV_32F);
//		taux.convertTo(Tvec ,CV_32F);

//		cv::Mat_<float> rotMat(3,3);
//		cv::Rodrigues(Rvec, rotMat);
//
//		cv::Mat tmp = cv::Mat::eye(4,4,CV_32F);
//
//		// Copy to transformation matrix
//		for (int col=0; col<3; col++)
//		{
//			for (int row=0; row<3; row++)
//			{
//				tmp.row(row).col(col) = rotMat(row,col); // Copy rotation component
//			}
//			tmp.col(3).row(col) = Tvec(col); // Copy translation component
//		}
//		tmp.col(3).row(3) = 1.0f;

//		cv::Mat cvToGl = cv::Mat::zeros(4, 4, CV_32F);
//		cvToGl.at<float>(0, 0) = 1.0f;
//		cvToGl.at<float>(1, 1) = -1.0f; // Invert the y axis
//		cvToGl.at<float>(2, 2) = -1.0f; // invert the z axis
//		cvToGl.at<float>(3, 3) = 1.0f;
//		*camPose = cvToGl * tmp;
	} else {
		*mv = Mat(Size(0,0),CV_8UC1);
		*proj = Mat(Size(0,0),CV_8UC1);
	}



//	__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "Calculations done in %f ms", ( std::clock() - start ) / ((double) CLOCKS_PER_SEC/1000));

//	moncleanup();

	return true;

//	imwrite("/sdcard/nonfree/markers.png", drawing);
}

JNIEXPORT void JNICALL Java_be_wouterfranken_arboardgame_rendering_tracking_CameraPose_getCameraCalibration(JNIEnv * env, jobject jobject, jlong camMatPtr) {
	Mat * cam = (Mat *) camMatPtr;

	*cam = calibData.intrinsics;
}
