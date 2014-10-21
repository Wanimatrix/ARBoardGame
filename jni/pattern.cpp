///*
// * pattern.cpp
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
//////////////////////////////////////////////////////////////////////
//// File includes:
//#include "pattern.hpp"
//#include <android/log.h>
//
//
//void PatternTrackingInfo::computePose(const Pattern& pattern, const cv::Mat& intrinsics, const cv::Mat& distortion)
//{
//  cv::Mat Rvec;
//  cv::Mat_<float> Tvec;
//  cv::Mat raux,taux;
//
//  cv::solvePnP(pattern.points3d, points2d, intrinsics, distortion,raux,taux);
//  raux.convertTo(Rvec,CV_32F);
//  taux.convertTo(Tvec ,CV_32F);
//
//  cv::Mat_<float> rotMat(3,3);
//  cv::Rodrigues(Rvec, rotMat);
//
//  pose3d = cv::Mat::eye(4,4,CV_32F);
//
//  // Copy to transformation matrix
//  for (int col=0; col<3; col++)
//  {
//    for (int row=0; row<3; row++)
//    {
//     pose3d.row(row).col(col) = rotMat(row,col); // Copy rotation component
//    }
//    pose3d.col(3).row(col) = Tvec(col); // Copy translation component
//  }
//
//  __android_log_print(ANDROID_LOG_DEBUG,APPNAME,"Copying done");
//
//  // Since solvePnP finds camera location, w.r.t to marker pose, to get marker pose w.r.t to the camera we invert it.
//  pose3d = pose3d.getInverted();
//}
//
//void PatternTrackingInfo::draw2dContour(cv::Mat& image, cv::Scalar color) const
//{
//  for (size_t i = 0; i < points2d.size(); i++)
//  {
//    cv::line(image, points2d[i], points2d[ (i+1) % points2d.size() ], color, 2, CV_AA);
//  }
//}
