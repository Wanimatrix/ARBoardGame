/*
 * app.hpp
 *
 *  Created on: 5-okt.-2014
 *      Author: gaming
 */

#ifndef APP_HPP_
#define APP_HPP_

#include <cameraparameters.h>


struct CalibrationData {
	cv::Mat intrinsics;
	cv::Mat distortion;
	cv::Size imgSize;
};



#endif /* APP_HPP_ */
