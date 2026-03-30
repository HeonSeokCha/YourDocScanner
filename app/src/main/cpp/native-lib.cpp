#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
#include <numeric>
#include <deque>
#include <android/bitmap.h>

#define LOG_TAG "CHS_123"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

static const double MIN_AREA_RATIO = 0.10;
static const double MAX_AREA_RATIO = 0.98;

static void autoCanny(const Mat &gray, Mat &edges, double sigma = 0.33) {
    Mat flat = gray.reshape(1);
    vector<uchar> vec(flat.begin<uchar>(), flat.end<uchar>());
    nth_element(vec.begin(), vec.begin() + vec.size() / 2, vec.end());
    double median = vec[vec.size() / 2];

    double lower = max(0.0, (1.0 - sigma) * median);
    double upper = min(255.0, (1.0 + sigma) * median);
    Canny(gray, edges, lower, upper);
}

static bool findDocContour(
        const vector<vector<Point>> &contours,
        double epsilonFactor,
        vector<Point2f> &result
) {
    for (const auto &contour: contours) {
        vector<Point2f> cf(contour.begin(), contour.end());
        double peri = arcLength(cf, true);
        vector<Point2f> approx;
        approxPolyDP(cf, approx, epsilonFactor * peri, true);

        if (approx.size() == 4 && isContourConvex(approx)) {
            result = approx;
            return true;
        }
    }
    return false;
}


Mat yuvToBgr(JNIEnv *env, jbyteArray yuvData, jint width, jint height) {
    jbyte *data = env->GetByteArrayElements(yuvData, nullptr);
    Mat yuv(height + height / 2, width, CV_8UC1, data);
    Mat bgr;
    cvtColor(yuv, bgr, COLOR_YUV2BGR_NV21);
    env->ReleaseByteArrayElements(yuvData, data, JNI_ABORT);
    return bgr;
}

bool validateQuad(
        const vector<Point2f> &poly,
        int imgWidth,
        int imgHeight
) {

    if (poly.size() != 4) return false;

    double imageArea = imgWidth * imgHeight;
    double area = contourArea(poly);

    double areaRatio = (area / imageArea);
    if (areaRatio < MIN_AREA_RATIO || areaRatio > MAX_AREA_RATIO) {
        return false;
    }
    return true;
}


extern "C" JNIEXPORT jobject JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_detectRectangles(
        JNIEnv *env,
        jobject thiz,
        jobject bitmapIn
) {
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmapIn, &info);

    void *pixels;
    AndroidBitmap_lockPixels(env, bitmapIn, &pixels);

    Mat src(info.height, info.width, CV_8UC4, pixels);

    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);

    Ptr<CLAHE> clahe = createCLAHE(2.0, Size(8, 8));
    clahe->apply(gray, gray);

    Mat filtered;
    bilateralFilter(gray, filtered, 9, 75.0, 75.0);

    Mat edges;
    autoCanny(filtered, edges, 0.33);

    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(edges, edges, kernel);

    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(edges, contours, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE);

    sort(contours.begin(), contours.end(), [](const auto &a, const auto &b) {
        return contourArea(a) > contourArea(b);
    });

    vector<Point2f> docContour;
    bool found = false;

    for (auto &contour: contours) {
        Mat contourF;
        Mat(contour).convertTo(contourF, CV_32F);

        double peri = arcLength(contourF, true);

        vector<Point2f> approx;
        approxPolyDP(contourF, approx, 0.02 * peri, true);

        if (validateQuad(approx, info.width, info.height)) {
            docContour = approx;
            found = true;
            break;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapIn);
    gray.release();
    edges.release();

    if (!found) return nullptr;

    jclass alClass = env->FindClass("java/util/ArrayList");
    jmethodID alInit = env->GetMethodID(alClass, "<init>", "()V");
    jmethodID alAdd = env->GetMethodID(alClass, "add", "(Ljava/lang/Object;)Z");
    jobject resultList = env->NewObject(alClass, alInit);

    jclass ptClass = env->FindClass("android/graphics/Point");
    jmethodID ptInit = env->GetMethodID(ptClass, "<init>", "(II)V");

    for (const auto &pt: docContour) {
        jobject point = env->NewObject(ptClass, ptInit,
                static_cast<jint>(pt.x),
                static_cast<jint>(pt.y));
        env->CallBooleanMethod(resultList, alAdd, point);
        env->DeleteLocalRef(point);
    }

    return resultList;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_detectRectanglesFromBitmap(
        JNIEnv *env,
        jobject thiz,
        jobject bitmapIn
) {
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmapIn, &info);

    void *pixels;
    AndroidBitmap_lockPixels(env, bitmapIn, &pixels);

    Mat src(info.height, info.width, CV_8UC4, pixels);

    Mat gray;
    cvtColor(src, gray, COLOR_RGBA2GRAY);

    Ptr<CLAHE> clahe = createCLAHE(2.0, Size(8, 8));
    clahe->apply(gray, gray);

    Mat filtered;
    bilateralFilter(gray, filtered, 9, 75.0, 75.0);

    Mat edges;
    autoCanny(filtered, edges, 0.33);

    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(edges, edges, kernel);

    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(edges, contours, hierarchy, RETR_LIST, CHAIN_APPROX_SIMPLE);

    sort(contours.begin(), contours.end(), [](const auto &a, const auto &b) {
        return contourArea(a) > contourArea(b);
    });

    vector<Point2f> docContour;
    bool found = false;

    for (auto &contour: contours) {
        Mat contourF;
        Mat(contour).convertTo(contourF, CV_32F);

        double peri = arcLength(contourF, true);

        vector<Point2f> approx;
        approxPolyDP(contourF, approx, 0.02 * peri, true);

        if (validateQuad(approx, info.width, info.height)) {
            docContour = approx;
            found = true;
            break;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmapIn);
    gray.release();
    edges.release();

    if (!found) return nullptr;

    jclass alClass = env->FindClass("java/util/ArrayList");
    jmethodID alInit = env->GetMethodID(alClass, "<init>", "()V");
    jmethodID alAdd = env->GetMethodID(alClass, "add", "(Ljava/lang/Object;)Z");
    jobject resultList = env->NewObject(alClass, alInit);

    jclass ptClass = env->FindClass("androidx/compose/ui/geometry/Offset");
    jmethodID ptInit = env->GetMethodID(ptClass, "<init>", "(II)V");

    for (const auto &pt: docContour) {
        jobject point = env->NewObject(ptClass, ptInit,
                static_cast<jint>(pt.x),
                static_cast<jint>(pt.y));
        env->CallBooleanMethod(resultList, alAdd, point);
        env->DeleteLocalRef(point);
    }

    return resultList;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_warpDocument(
        JNIEnv *env,
        jobject,
        jobject bitmapIn,
        jfloatArray points,
        jboolean isFlip
) {
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmapIn, &info);

    void *pixels;
    AndroidBitmap_lockPixels(env, bitmapIn, &pixels);

    Mat src(info.height, info.width, CV_8UC4, pixels);
    Mat srcBgr;
    cvtColor(src, srcBgr, COLOR_RGBA2BGR);
    AndroidBitmap_unlockPixels(env, bitmapIn);

    jfloat *pts = env->GetFloatArrayElements(points, nullptr);
    vector<Point2f> srcPts = {
            {pts[0], pts[1]},  // TL
            {pts[2], pts[3]},  // TR
            {pts[4], pts[5]},  // BR
            {pts[6], pts[7]}   // BL
    };
    env->ReleaseFloatArrayElements(points, pts, JNI_ABORT);

    double topW = norm(srcPts[1] - srcPts[0]);
    double bottomW = norm(srcPts[2] - srcPts[3]);
    double leftH = norm(srcPts[3] - srcPts[0]);
    double rightH = norm(srcPts[2] - srcPts[1]);

    int dstW = static_cast<int>((topW + bottomW) / 2.0);
    int dstH = static_cast<int>((leftH + rightH) / 2.0);
    if (dstW <= 0 || dstH <= 0) return nullptr;

    vector<Point2f> dstPts = {
            {0.f, 0.f},  // TL
            {(float) dstW, 0.f},  // TR
            {(float) dstW, (float) dstH},  // BR
            {0.f, (float) dstH}   // BL
    };

    Mat M = getPerspectiveTransform(srcPts, dstPts);
    Mat warped;
    warpPerspective(srcBgr, warped, M, Size(dstW, dstH),
            INTER_LINEAR, BORDER_CONSTANT, Scalar(255, 255, 255));

    if (isFlip) {
        flip(warped, warped, 1);
    }

    Mat enhanced;
    Mat lab;
    cvtColor(warped, lab, COLOR_BGR2Lab);
    vector<Mat> channels;
    split(lab, channels);
    Ptr<CLAHE> clahe = createCLAHE(2.0, Size(8, 8));
    clahe->apply(channels[0], channels[0]);
    merge(channels, lab);
    cvtColor(lab, enhanced, COLOR_Lab2BGR);

    Mat rgba;
    cvtColor(enhanced, rgba, COLOR_BGR2RGBA);

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    jclass configClass = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888 = env->GetStaticFieldID(
            configClass, "ARGB_8888",
            "Landroid/graphics/Bitmap$Config;");
    jobject config = env->GetStaticObjectField(configClass, argb8888);
    jmethodID createBitmap = env->GetStaticMethodID(
            bitmapClass, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)"
            "Landroid/graphics/Bitmap;");
    jobject bitmapOut = env->CallStaticObjectMethod(
            bitmapClass, createBitmap, dstW, dstH, config);

    AndroidBitmapInfo outInfo;
    AndroidBitmap_getInfo(env, bitmapOut, &outInfo);

    void *outPixels;
    AndroidBitmap_lockPixels(env, bitmapOut, &outPixels);

    for (int row = 0; row < dstH; ++row) {
        uint8_t *dst = static_cast<uint8_t *>(outPixels) + row * outInfo.stride;
        uint8_t *src = rgba.data + row * rgba.step[0];
        memcpy(dst, src, dstW * 4);
    }

    AndroidBitmap_unlockPixels(env, bitmapOut);

    return bitmapOut;
}
