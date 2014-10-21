/*
 * app.hpp
 *
 *  Created on: 5-okt.-2014
 *      Author: gaming
 */

#ifndef APP_HPP_
#define APP_HPP_

//#include "pattern.hpp"
//#include "pattern_detector.hpp"
#include <cameraparameters.h>

//Pattern p;
//PatternDetector pd;
//PatternTrackingInfo pti;

struct CalibrationData {
	cv::Mat intrinsics;
	cv::Mat distortion;
	cv::Size imgSize;
};



#endif /* APP_HPP_ */
