#include <iostream>
#include <vector>
#include <opencv2/opencv.hpp>
#include <unistd.h>
#include <cmath>
#include <string>

#include "detect.hpp"
#include "features.hpp"
#include "showHOG.hpp"
#include "logger.hpp"

#define LOG_TAG "HOGDETECT"

using namespace std;
using namespace cv;

/*
TODO
 * This code -> Android
 * Fix Scaling problem using marker scales or other (afwijking kan GROOT zijn!)?
 * Fix multiple blocks problem using post-processing
*/

static int phiValues[] = {20,40};
static int thetaValues[] = {0,10,20,30,40,50,60,70,80,90};
static vector<Mat> renderedImages;

static vector<vector<Mat> > featuresRendered;
static vector<vector<double> > scalesRendered;

static vector<Mat> featuresInput;
static vector<double> scaleInput;

static Mat input;

void HogDetector::loadRenderedImages(string dir) {
    char s[1000];

	int i = 0;
	for (int *phi = phiValues; phi != phiValues+(sizeof(phiValues)/sizeof(int)); ++phi) {
    	for (int *theta = thetaValues; theta != thetaValues+(sizeof(thetaValues)/sizeof(int)); ++theta, ++i) {
			sprintf(s, "image_%03d_p%03d_t%03d_r%03d.png", i, *phi, *theta, 1);
			string path = dir + string("/") + string(s);

			renderedImages.push_back(imread(path.c_str()));
		}
    }

    struct HogParams params = {5,0.1,0.1,0.1,3};

    for(int i = 0; i < renderedImages.size(); i++) {
        vector<Mat> tmpFeat;
        vector<double> tmpScale;
        Mat edges;
        vector<vector<Point> > contours;
        vector<Vec4i> hierarchy;
        Canny(renderedImages[i], edges, 50, 50*3);

        LOGD("i: %d\n",i);

        LOGD("Edges Old Type: %d\n",edges.type());
        cvtColor(edges,edges,CV_GRAY2RGB);
        edges.convertTo(edges, CV_64FC3);
        LOGD("Edges New Type: %d == %d\n",edges.type(),CV_64FC3);

        hogPyramid(edges, params, tmpFeat, tmpScale);
        featuresRendered.push_back(tmpFeat);
        scalesRendered.push_back(tmpScale);
    }

    Mat hogPic = HOGPicture(featuresRendered[14][0],10,params.sbin);
    imwrite("/sdcard/arbg/hogPic.jpg",hogPic);
}

vector<ResultingMatch> HogDetector::findBestMatches(Mat input) {
    // namedWindow("window");

    Mat image;
    cv::GaussianBlur(input, image, cv::Size(3, 3), 3);
    cv::addWeighted(input, 1.5, image, -0.5, 0, input);

    Mat mask;
    inRange(input, Scalar(0, 0, 0), Scalar(40,40,40), mask);

    Mat noisy;
    image.copyTo(noisy);
    randn(noisy, 125, 50);

    int N = 10;
	Mat kernel = getStructuringElement(MORPH_ELLIPSE, Point(N, N));
    morphologyEx(mask, mask, MORPH_DILATE, kernel);

    bitwise_not(mask, mask);

    Mat edges;
    Canny(input, edges, 50, 50*3);

    bitwise_and(edges, mask, edges);

    imwrite("/sdcard/arbg/edges.png",edges);

    cvtColor(edges,edges,CV_GRAY2RGB);
    edges.convertTo(edges, CV_64FC3);

    struct HogParams params = {5,0.3,2.0,0.1,3};

    hogPyramid(edges, params, featuresInput, scaleInput);

    Mat tmp;
    vector<float> best;
    vector<Mat> bestMat;
    vector<Size> bestSize;
    vector<ResultingMatch> resultVec;
    Mat matchResult;
    double max, min;
    Point minPoint, maxPoint;
    int amount = 1;

    vector<Mat> inputBins;

    for(int scaleIdx = 0; scaleIdx < featuresInput.size(); scaleIdx++) {
        Mat scaleResult;
        double bestScore = 0;
        int bestVpIdx;

        featuresInput[scaleIdx].convertTo(tmp,CV_32FC9);
        split(tmp,inputBins);

        for(int vpIdx = 0; vpIdx < featuresRendered.size(); vpIdx++) {
            Mat templ = featuresRendered[vpIdx][0];

            vector<Mat> templateBins;
            templ.convertTo(tmp,CV_32FC9);
            split(tmp,templateBins);

            vector<Mat> tmpResult(templateBins.size());
            // Template is too small: take the next template
            if(inputBins[0].rows-templateBins[0].rows+1 <= 0 || inputBins[0].cols-templateBins[0].cols+1 <= 0) continue;
            LOGD("Scale: %d\n",scaleIdx);
            Mat result = Mat::zeros(inputBins[0].rows-templateBins[0].rows+1,inputBins[0].cols-templateBins[0].cols+1,CV_32FC1);

            for(int i = 0; i < templateBins.size(); i++) {
                matchTemplate(inputBins[i],templateBins[i],tmpResult[i],CV_TM_CCOEFF);
                result += tmpResult[i];
            }
            result /= templateBins.size();

            minMaxLoc(result, &min, &max, &minPoint, &maxPoint);

            LOGD("Max: %lf, currentBestScore: %lf\n",max,bestScore);
            LOGD("Theta: %d, phi: %d\n",thetaValues[vpIdx%(sizeof(thetaValues)/sizeof(int))],
            phiValues[vpIdx/(sizeof(thetaValues)/sizeof(int))]);

            if(max > bestScore) {
                LOGD("Better result was found!\n");
                scaleResult = result.clone();
                bestScore = max;
                bestVpIdx = vpIdx;
            }
        }

        if(bestScore == 0) continue;

        Mat bestTemplate = featuresRendered[bestVpIdx][0];

        double scale = ((featuresInput[scaleIdx].size().width*5.0)/input.size().width);
        Size newSize(bestTemplate.size().width*5*(1.0/scale),bestTemplate.size().height*5*(1.0/scale));

        resize(bestTemplate,bestTemplate, newSize);

        minMaxLoc(scaleResult, &min, &max, &minPoint, &maxPoint);

        // resize(input,matchResult,featuresInput[scaleIdx].size()*5);
        matchResult = input.clone();
        best.push_back(max);
        bestMat.push_back(matchResult);
        bestSize.push_back(bestTemplate.size());
        resultVec.push_back(ResultingMatch());
        resultVec.back().rho = 1.5f;
        resultVec.back().phi = phiValues[bestVpIdx/(sizeof(thetaValues)/sizeof(int))];
        resultVec.back().theta = thetaValues[bestVpIdx%(sizeof(thetaValues)/sizeof(int))];
        resultVec.back().score = max;
        resultVec.back().location = maxPoint*5*(1.0/scale);
        resultVec.back().templateSize = bestTemplate.size();

        normalize(scaleResult,scaleResult,0,1,CV_MINMAX);

        Mat loopResult = scaleResult.clone();

        cvtColor(scaleResult,scaleResult,CV_GRAY2RGB);
        Mat tmpResult2;
        resize(scaleResult,tmpResult2,scaleResult.size()*5,0,0,INTER_AREA);
        for(int i = 0; i < amount; i++) {
            minMaxLoc(loopResult, &min, &max, &minPoint, &maxPoint);
            loopResult.at<float>(maxPoint.y,maxPoint.x) = 0;
            Point rescaled_maxPoint = maxPoint*5*(1.0/scale);
            rectangle(matchResult,rescaled_maxPoint,
                            Point(rescaled_maxPoint.x+bestTemplate.cols,rescaled_maxPoint.y+bestTemplate.rows),Scalar(0,0,255));
            scaleResult.at<Vec3f>(maxPoint.y, maxPoint.x) = Vec3f(0,0,255);
        }
    }

    for(int i = 0; i < best.size();i++) {

        char filename[34];
        sprintf(filename,"/sdcard/arbg/hogMatchResult%02d.png",i);

        imwrite(filename,bestMat[i]);

        LOGD("Best: %f\n", best[i]);
        LOGD("Best size: [%d x %d]\n", bestSize[i].width, bestSize[i].height);
        LOGD("New Best: %f\n", best[i]/bestSize[i].width);
        LOGD("The Result: \n");
        LOGD("  Rho: %f\n", resultVec[i].rho);
        LOGD("  Theta: %d\n", resultVec[i].theta);
        LOGD("  Phi: %d\n", resultVec[i].phi);
        LOGD("  Size: [%d x %d]\n", resultVec[i].templateSize.width, resultVec[i].templateSize.height);
        LOGD("  Location: (%d,%d)\n", resultVec[i].location.x, resultVec[i].location.y);
    }

    return resultVec;
}
