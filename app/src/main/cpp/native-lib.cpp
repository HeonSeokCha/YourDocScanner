#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
#include <numeric>
#include <deque>
#include <android/bitmap.h>

#define LOG_TAG "DocScanner"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

static const double MIN_AREA_RATIO = 0.10;
static const double MAX_AREA_RATIO = 0.98;
static const double MIN_ANGLE_DEG = 60.0;
static const double MAX_ANGLE_DEG = 120.0;
static const double MIN_ASPECT_RATIO = 0.3;
static const double MAX_ASPECT_RATIO = 3.5;
static const double CONVEXITY_THRESHOLD = 0.90;
static const int SMOOTH_HISTORY = 5;

static deque<vector<Point2f>> g_history;

Mat yuvToBgr(JNIEnv *env, jbyteArray yuvData, jint width, jint height) {
    jbyte *data = env->GetByteArrayElements(yuvData, nullptr);
    Mat yuv(height + height / 2, width, CV_8UC1, data);
    Mat bgr;
    cvtColor(yuv, bgr, COLOR_YUV2BGR_NV21);
    env->ReleaseByteArrayElements(yuvData, data, JNI_ABORT);
    return bgr;
}

Mat preprocess(const Mat &bgr) {
    Mat gray;
    cvtColor(bgr, gray, COLOR_BGR2GRAY);

    Ptr<CLAHE> clahe = createCLAHE(2.0, Size(8, 8));
    clahe->apply(gray, gray);

    Mat bilateral;
    bilateralFilter(gray, bilateral, 9, 75, 75);

    Mat otsuMat;
    double otsuVal = threshold(bilateral, otsuMat, 0, 255,
            THRESH_BINARY | THRESH_OTSU);
    double lowThresh = otsuVal * 0.5;
    double highThresh = otsuVal;

    Mat edges;
    Canny(bilateral, edges, lowThresh, highThresh);

    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(edges, edges, kernel, Point(-1, -1), 2);

    return edges;
}

struct QuadScore {
    vector<Point> poly;
    double score;
};

bool validateQuad(const vector<Point> &poly,
        int imgWidth, int imgHeight,
        QuadScore &outScore) {

    if (poly.size() != 4) return false;

    double imageArea = imgWidth * imgHeight;
    double area = contourArea(poly);

    double areaRatio = area / imageArea;
    if (areaRatio < MIN_AREA_RATIO || areaRatio > MAX_AREA_RATIO) return false;

    vector<Point> hull;
    convexHull(poly, hull);
    double hullArea = contourArea(hull);
    double convexity = area / (hullArea + 1e-6);
    if (convexity < CONVEXITY_THRESHOLD) return false;

    double angleScore = 0.0;
    for (int i = 0; i < 4; i++) {
        Point v1 = poly[i] - poly[(i + 1) % 4];
        Point v2 = poly[(i + 2) % 4] - poly[(i + 1) % 4];
        double cosA = v1.dot(v2) / (norm(v1) * norm(v2) + 1e-6);
        double angle = acos(clamp(cosA, -1.0, 1.0)) * 180.0 / CV_PI;
        if (angle < MIN_ANGLE_DEG || angle > MAX_ANGLE_DEG) return false;
        angleScore += 1.0 - abs(angle - 90.0) / 90.0;
    }
    angleScore /= 4.0;

    double w1 = norm(poly[0] - poly[1]);
    double w2 = norm(poly[2] - poly[3]);
    double h1 = norm(poly[1] - poly[2]);
    double h2 = norm(poly[3] - poly[0]);
    double width = (w1 + w2) / 2.0;
    double height = (h1 + h2) / 2.0;
    if (width < 1.0 || height < 1.0) return false;
    double aspect = max(width, height) / min(width, height);
    if (aspect < MIN_ASPECT_RATIO || aspect > MAX_ASPECT_RATIO) return false;

    outScore.score = areaRatio * 0.7 + angleScore * 0.3;
    outScore.poly = poly;
    return true;
}

vector<Point2f> sortCornersCW(const vector<Point> &poly) {
    Point2f center(0, 0);
    for (const auto &p: poly)
        center += Point2f(p.x, p.y);
    center *= (1.0f / 4.0f);

    vector<pair<double, Point2f>> anglePoint;
    for (const auto &p: poly) {
        double angle = atan2(p.y - center.y, p.x - center.x);
        anglePoint.push_back({angle, Point2f(p.x, p.y)});
    }
    sort(anglePoint.begin(), anglePoint.end(),
            [](const auto &a, const auto &b) {
                return a.first < b.first;
            });

    vector<Point2f> result;
    for (const auto &ap: anglePoint)
        result.push_back(ap.second);

    return result;
}

void refineCorners(const Mat &gray, vector<Point2f> &corners) {
    if (corners.empty()) return;
    TermCriteria criteria(TermCriteria::EPS | TermCriteria::MAX_ITER, 30, 0.01);
    cornerSubPix(gray, corners, Size(5, 5), Size(-1, -1), criteria);
}

QuadScore detectAtScale(const Mat &edges, const Mat &gray,
        int origWidth, int origHeight,
        float scale) {
    Mat scaledEdges;
    if (scale != 1.0f) {
        resize(edges, scaledEdges, Size(), scale, scale, INTER_LINEAR);
    } else {
        scaledEdges = edges;
    }

    vector<vector<Point>> contours;
    findContours(scaledEdges, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    QuadScore best;
    best.score = -1.0;

    for (const auto &contour: contours) {
        double perimeter = arcLength(contour, true);
        if (perimeter < 100) continue;

        vector<Point> poly;
        approxPolyDP(contour, poly, 0.02 * perimeter, true);

        QuadScore qs;
        int scaledW = static_cast<int>(origWidth * scale);
        int scaledH = static_cast<int>(origHeight * scale);

        if (validateQuad(poly, scaledW, scaledH, qs)) {
            if (qs.score > best.score) {
                if (scale != 1.0f) {
                    for (auto &p: qs.poly) {
                        p.x = static_cast<int>(p.x / scale);
                        p.y = static_cast<int>(p.y / scale);
                    }
                }
                best = qs;
            }
        }
    }
    return best;
}


vector<Point2f> smoothWithHistory(const vector<Point2f> &current) {
    g_history.push_back(current);
    if (g_history.size() > SMOOTH_HISTORY)
        g_history.pop_front();

    vector<Point2f> smoothed(4, Point2f(0, 0));
    for (const auto &frame: g_history) {
        for (int i = 0; i < 4; i++)
            smoothed[i] += frame[i];
    }
    float n = static_cast<float>(g_history.size());
    for (auto &p: smoothed) p *= (1.0f / n);

    return smoothed;
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

    Mat edges = preprocess(bgr);

    Mat gray;
    cvtColor(bgr, gray, COLOR_BGR2GRAY);

    static const float SCALES[] = {1.0f, 0.75f, 0.5f};
    QuadScore best;
    best.score = -1.0;

    for (float scale: SCALES) {
        QuadScore qs = detectAtScale(edges, gray, width, height, scale);
        if (qs.score > best.score) {
            best = qs;
        }
        if (best.score > 0.8) break;
    }

    if (best.poly.empty()) {
        g_history.clear();
        return env->NewFloatArray(0);
    }

    vector<Point2f> corners = sortCornersCW(best.poly);

    refineCorners(gray, corners);

    corners = smoothWithHistory(corners);

    jfloatArray result = env->NewFloatArray(9);
    jfloat pts[9] = {
            corners[0].x, corners[0].y,  // TL
            corners[1].x, corners[1].y,  // TR
            corners[2].x, corners[2].y,  // BR
            corners[3].x, corners[3].y,  // BL
            static_cast<jfloat>(best.score)
    };
    env->SetFloatArrayRegion(result, 0, 9, pts);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_resetHistory(
        JNIEnv *, jobject
) {
    g_history.clear();
}


extern "C" JNIEXPORT jobject JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_warpDocument(
        JNIEnv *env,
        jobject,
        jobject bitmapIn,
        jfloatArray points
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

    double topW    = norm(srcPts[1] - srcPts[0]);
    double bottomW = norm(srcPts[2] - srcPts[3]);
    double leftH   = norm(srcPts[3] - srcPts[0]);
    double rightH  = norm(srcPts[2] - srcPts[1]);

    int dstW = static_cast<int>((topW + bottomW) / 2.0);
    int dstH = static_cast<int>((leftH + rightH) / 2.0);

    if (dstW <= 0 || dstH <= 0) return nullptr;

    vector<Point2f> dstPts = {
            {0.f,            0.f           },  // TL
            {(float)dstW,    0.f           },  // TR
            {(float)dstW,    (float)dstH   },  // BR
            {0.f,            (float)dstH   }   // BL
    };

    Mat M   = getPerspectiveTransform(srcPts, dstPts);
    Mat dst;
    warpPerspective(srcBgr, dst, M, Size(dstW, dstH),
            INTER_LINEAR, BORDER_CONSTANT, Scalar(255, 255, 255));

    Mat rgba;
    cvtColor(dst, rgba, COLOR_BGR2RGBA);

    jclass bitmapClass   = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmap = env->GetStaticMethodID(
            bitmapClass, "createBitmap",
            "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;"
    );
    jclass configClass   = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb8888    = env->GetStaticFieldID(configClass, "ARGB_8888",
            "Landroid/graphics/Bitmap$Config;");
    jobject config       = env->GetStaticObjectField(configClass, argb8888);
    jobject bitmapOut    = env->CallStaticObjectMethod(
            bitmapClass, createBitmap, dstW, dstH, config
    );

    void *outPixels;
    AndroidBitmap_lockPixels(env, bitmapOut, &outPixels);
    memcpy(outPixels, rgba.data, rgba.total() * rgba.elemSize());
    AndroidBitmap_unlockPixels(env, bitmapOut);

    return bitmapOut;
}