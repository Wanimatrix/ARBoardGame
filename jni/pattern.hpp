///*
// * pattern.hpp
// *
// *  Created on: 5-okt.-2014
// *      Author: Wouter Franken
// *
// *  *****************************************
// *   	COPYRIGHT / INSPIRED BY
// *  *****************************************
// *  Khvedchenia Ievgen
// *  Ch3 of the book "Mastering OpenCV with Practical Computer Vision Projects"
// *  Copyright Packt Publishing 2012.
// *  http://www.packtpub.com/cool-projects-with-opencv/book
// *
// *  *****************************************
// */
//
//#ifndef PATTERN_HPP_
//#define PATTERN_HPP_
//
//#define APPNAME "be.wouterfranken.arboardgame"
//
//////////////////////////////////////////////////////////////////////
//// File includes:
//
//#include <opencv2/opencv.hpp>
//
///**
// * Store the image data and computed descriptors of target pattern
// */
//struct Pattern
//{
//  cv::Size                  size;
//  cv::Mat                   frame;
//  cv::Mat                   grayImg;
//
//  std::vector<cv::KeyPoint> keypoints;
//  cv::Mat                   descriptors;
//
//  std::vector<cv::Point2f>  points2d;
//  std::vector<cv::Point3f>  points3d;
//};
//
///**
// * Intermediate pattern tracking info structure
// */
//struct PatternTrackingInfo
//{
//  cv::Mat                   homography;
//  std::vector<cv::Point2f>  points2d;
//  cv::Mat            		pose3d;
//
//  std::string test;
//
//  void draw2dContour(cv::Mat& image, cv::Scalar color) const;
//
//  /**
//   * Compute pattern pose using PnP algorithm
//   */
//  void computePose(const Pattern& pattern, const cv::Mat& intrinsics, const cv::Mat& distortion);
//};


#endif /* PATTERN_HPP_ */
