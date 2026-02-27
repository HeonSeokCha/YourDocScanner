#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>

using namespace cv;
using namespace std;

Mat yuvToBgr(JNIEnv *env, jbyteArray yuvData, jint width, jint height) {
    jsize length = env->GetArrayLength(yuvData);
    jbyte *data = env->GetByteArrayElements(yuvData, nullptr);

    Mat yuv(height + height / 2, width, CV_8UC1, data);
    Mat bgr;
    cvtColor(yuv, bgr, COLOR_YUV2BGR_NV21);

    env->ReleaseByteArrayElements(yuvData, data, JNI_ABORT);
    return bgr;
}

bool isRectangle(const vector<Point> &poly, double minArea = 5000.0) {
    if (poly.size() != 4) return false;
    double area = contourArea(poly);
    if (area < minArea) return false;

    for (int i = 0; i < 4; i++) {
        Point p1 = poly[i];
        Point p2 = poly[(i + 1) % 4];
        Point p3 = poly[(i + 2) % 4];

        Point v1 = p1 - p2;
        Point v2 = p3 - p2;

        double cosAngle = v1.dot(v2) / (norm(v1) * norm(v2) + 1e-6);
        double angle = acos(clamp(cosAngle, -1.0, 1.0)) * 180.0 / CV_PI;

        if (angle < 70.0 || angle > 110.0) return false;
    }
    return true;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_detectRectangles(
        JNIEnv *env,
        jobject,
        jbyteArray yuvData,
        jint width,
        jint height
) {

    Mat bgr = yuvToBgr(env, yuvData, width, height);
    Mat gray, blurred, edges;

    cvtColor(bgr, gray, COLOR_BGR2GRAY);
    GaussianBlur(gray, blurred, Size(5, 5), 0);
    Canny(blurred, edges, 50, 150);

    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(edges, edges, kernel);

    vector<vector<Point>> contours;
    findContours(edges, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    vector<Point> bestRect;
    double bestArea = 0;

    for (const auto &contour : contours) {
        double perimeter = arcLength(contour, true);
        vector<Point> poly;
        approxPolyDP(contour, poly, 0.02 * perimeter, true);

        if (isRectangle(poly)) {
            double area = contourArea(poly);
            if (area > bestArea) {
                bestArea = area;
                bestRect = poly;
            }
        }
    }

    if (bestRect.empty()) {
        return env->NewFloatArray(0);
    }

    sort(bestRect.begin(), bestRect.end(),
            [](const Point &a, const Point &b) {
                return a.y < b.y || (a.y == b.y && a.x < b.x);
            });

    jfloatArray result = env->NewFloatArray(8);
    jfloat points[8] = {
            (jfloat) bestRect[0].x, (jfloat) bestRect[0].y,
            (jfloat) bestRect[1].x, (jfloat) bestRect[1].y,
            (jfloat) bestRect[2].x, (jfloat) bestRect[2].y,
            (jfloat) bestRect[3].x, (jfloat) bestRect[3].y,
    };
    env->SetFloatArrayRegion(result, 0, 8, points);
    return result;
}