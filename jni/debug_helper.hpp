/*
 * debug_helper.hpp
 *
 *  Created on: 7-okt.-2014
 *      Author: gaming
 */

#ifndef DEBUG_HELPER_HPP_
#define DEBUG_HELPER_HPP_

#define _DEBUG 0

namespace cv
{
	// Draw matches between two images
   inline cv::Mat getMatchesImage(cv::Mat query, cv::Mat pattern, const std::vector<cv::KeyPoint>& queryKp, const std::vector<cv::KeyPoint>& trainKp, std::vector<cv::DMatch> matches, int maxMatchesDrawn)
   {
       cv::Mat outImg;

       if (matches.size() > maxMatchesDrawn)
       {
           matches.resize(maxMatchesDrawn);
       }

       cv::drawMatches
           (
           query,
           queryKp,
           pattern,
           trainKp,
           matches,
           outImg,
           cv::Scalar(0,200,0,255),
           cv::Scalar::all(-1),
           std::vector<char>(),
           cv::DrawMatchesFlags::NOT_DRAW_SINGLE_POINTS
           );

       return outImg;
   }
}

std::vector<std::clock_t> start;

#endif /* DEBUG_HELPER_HPP_ */
