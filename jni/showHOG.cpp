#include "logger.hpp"
#include "showHOG.hpp"

#define CV_64FC9 CV_MAKETYPE(CV_64F,9)
#define CV_64FC18 CV_MAKETYPE(CV_64F,18)

#define LOG_TAG "SHOWHOG"

Mat rotateImage(const Mat source, double angle,int border=20)
{
    Mat bordered_source;
    int top,bottom,left,right;
    top=bottom=left=right=border;
    copyMakeBorder( source, bordered_source, top, bottom, left, right, BORDER_CONSTANT,cv::Scalar() );
    Point2f src_center(bordered_source.cols/2.0F, bordered_source.rows/2.0F);
    Mat rot_mat = getRotationMatrix2D(src_center, angle, 1.0);
    Mat dst;
    warpAffine(bordered_source, dst, rot_mat, bordered_source.size());
    return dst;
}

Mat HOGPicture(Mat& w, int bs, int cs) {
    int cnt = (bs/cs);
    LOGD("Cnt: %d\n",cnt);

    Mat bim1 = Mat::zeros(bs, bs, CV_64FC1);
    bim1(Range(0,bim1.rows),Range(round(bs/2.0),round(bs/2.0)+2)).setTo(255);
    Mat bim =  Mat::zeros(bim1.rows, bim1.cols, CV_64FC9);
    vector<Mat> channels;
    split(bim, channels);
    channels[0] = bim1;
    channels.push_back(channels[0]);
    for(int i = 2; i <= 9;i++) {
        channels[i-1] = rotateImage(bim1, -(i-1)*20, 0);
        channels.push_back(channels[i-1]);
    }
    bim = Mat(bim.rows,bim.cols,CV_64FC18);
    // // merge(channels,18,bim);
    //
    Size s = w.size();
    LOGD("Weights size: [%d x %d]\n",s.width,s.height);
    vector<Mat> wChannels;
    split(w,wChannels);
    Mat im = Mat::zeros(cs*s.height, cs*s.width, CV_64FC1);
    for(int i = 0; i*bs+bs < im.rows;i++) {
        Range iis(i*bs,i*bs+bs);
        for(int j = 0; j*bs+bs < im.cols; j++) {
            Range jjs(j*bs,j*bs+bs);
            for(int k = 0; k < 9;k++) {
                // cout << "i: " << i << ", j: " << j << endl;
                Mat weights = Mat(1,cnt*2,CV_64FC1);
                for(int l = 0; l < cnt*2; l++) {
                    double wEl = *(((double *)wChannels[k].data) + (j*cnt+(l%cnt)) + (i*cnt+l/cnt)*wChannels[k].step1());
                    if(k == 0) LOGD("i: %d, j: %d\n",(i*cnt+l/cnt),(j*cnt+(l%cnt)));
                    if(wEl != wChannels[k].at<double>(i*cnt+l/cnt,j*cnt+(l%cnt))) {
                        LOGD("DIMENSION ERROR\n");
                        exit(1);
                    }

                    if(wEl > 0) {
                        LOGD("Weight: %lf\n",255*wEl);
                    }
                    weights.at<double>(0,l) = wEl;
                }

                // cout << "Result type: " << im.type() << channels[k].type() << endl;
                // cout << "iis: From " << iis.start << ", To: " << iis.end << endl;
                // cout << "jjs: From " << jjs.start << ", To: " << jjs.end << endl;
                // cout << "Piece size: " << im(iis,jjs).size() << endl;
                // if(wEl > 0) {
                //     cout << "Weight: " << 255*wEl << endl;
                // }
                addWeighted(im(iis,jjs),1,channels[k],mean(weights)[0],0,im(iis,jjs));
                // Mat test = im(iis,jjs) + channels[k]*wEl;
                // test.copyTo(im(iis,jjs));
            }
        }
    }

    Mat grayScale;
    im.convertTo(grayScale,CV_8UC1);

    Mat blue, green,red;
    threshold(grayScale,blue,0.5*255, 255,2);
    blue = 255-blue*2;

    threshold(grayScale,green,0.25*255, 255,3);
    Mat tmp;
    threshold(grayScale,tmp,0.75*255,255,2);
    bitwise_or(green,tmp,green);
    green = 255 - 4*abs(green - 0.5*255);

    threshold(grayScale,red,0.5*255, 255,3);
    red = (red-0.5*255)*2;

    vector<Mat> rgbChannels;
    rgbChannels.push_back(blue);
    rgbChannels.push_back(green);
    rgbChannels.push_back(red);
    Mat result;
    merge(rgbChannels,result);
    return result;
}
