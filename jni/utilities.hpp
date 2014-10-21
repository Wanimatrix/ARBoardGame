/*
 * utilities.hpp
 *
 *  Created on: 5-okt.-2014
 *      Author: Wouter Franken
 */

#ifndef UTILITIES_HPP_
#define UTILITIES_HPP_

#define APPNAME "be.wouterfranken.arboardgame"



class Utilities {
public:
	static void logMat(const cv::Mat& m, const std::string& matName);

	static void loadMatFromFile(const std::string& path, const std::string& matName, cv::Mat& result);

	static void saveMat(const std::string& path, const std::string& matName, const cv::Mat& mat);

	static void loadCameraCalibration(const std::string& path, cv::Mat& cameraMatrix, cv::Mat& distortionCoefficients);

	static void cvtKeyPtoP(const std::vector<cv::KeyPoint>& kpts, std::vector<cv::Point2f>& points);

	static void cvtPtoKpts(const std::vector<cv::Point2f>& points, std::vector<cv::KeyPoint>& kpts);
};

#endif /* UTILITIES_HPP_ */
