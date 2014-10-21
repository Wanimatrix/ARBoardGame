/*
 * utilities.cpp
 *
 *  Created on: 5-okt.-2014
 *      Author: Wouter Franken
 */

#include <vector>
#include <string>
#include <opencv2/core/core.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <android/log.h>
#include <iostream>
#include "utilities.hpp"
#include "app.hpp"

using namespace cv;
using namespace std;

CalibrationData calibData;

void Utilities::logMat(const Mat& m, const string& matName) {
	std::stringstream buffer;
	buffer << matName << ": " << m << endl;
	__android_log_print(ANDROID_LOG_WARN, APPNAME, "%s", buffer.str().c_str());
	buffer.str("");
	buffer.clear();
}

void Utilities::loadMatFromFile(const string& path, const string& matName, Mat& result) {
	FileStorage fs;
	fs.open(path, FileStorage::READ);

	if(fs.isOpened()) {
		__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "File %s is open",path.c_str());
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "File %s is not open",path.c_str());
	}

	fs[matName] >> result;
	fs.release();
}

void Utilities::saveMat(const string& path, const string& matName, const Mat& mat) {
	FileStorage fs;
	fs.open(path, FileStorage::WRITE);
	if(fs.isOpened()) {
		__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "File %s is open",path.c_str());
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "File %s is not open",path.c_str());
	}

	fs << matName << mat;
	fs.release();
}

void Utilities::loadCameraCalibration(const string& path, Mat& cameraMatrix, Mat& distortionCoefficients) {
	FileStorage fs;
	fs.open(path, FileStorage::READ);

	if(fs.isOpened()) {
		__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "File %s is open",path.c_str());
	} else {
		__android_log_print(ANDROID_LOG_DEBUG, APPNAME, "File %s is not open",path.c_str());
	}

	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"cam saved");

	fs["camera_matrix"] >> calibData.intrinsics;

	__android_log_print(ANDROID_LOG_DEBUG,APPNAME,"CameraMatrix loaded ...");

	fs["distortion_coefficients"] >> calibData.distortion;

	int width;
	int height;
	fs["image_width"] >> width;
	fs["image_height"] >> height;

	calibData.imgSize = Size(width,height);
	fs.release();
}

void Utilities::cvtKeyPtoP(const vector<KeyPoint>& kpts, vector<Point2f>& points) {
	points.clear();
	for (int i=0; i<kpts.size(); i++) points.push_back(kpts[i].pt);
}

void Utilities::cvtPtoKpts(const vector<Point2f>& points,vector<KeyPoint>& kpts) {
	kpts.clear();
	for (int i=0; i<points.size(); i++) kpts.push_back(KeyPoint(points[i],1));
}
