#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
#include <deque>

#define LOG_TAG "_DEBUG"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

using namespace cv;
using namespace std;

static const double MIN_AREA_RATIO      = 0.05;
static const double MAX_AREA_RATIO      = 0.98;
static const double MIN_ANGLE_DEG       = 50.0;
static const double MAX_ANGLE_DEG       = 130.0;
static const double MIN_ASPECT_RATIO    = 0.2;
static const double MAX_ASPECT_RATIO    = 5.0;
static const double CONVEXITY_THRESHOLD = 0.80;
static const int    SMOOTH_HISTORY      = 5;

static deque<vector<Point2f>> g_history;


Mat yuvToBgr(JNIEnv *env,
        jbyteArray yData,   jint yRowStride,
        jbyteArray uvData,  jint uvRowStride, jint uvPixelStride,
        jint width, jint height) {

    jsize yLen  = env->GetArrayLength(yData);
    jbyte *yPtr = env->GetByteArrayElements(yData, nullptr);

    int frameSize = width * height;
    vector<uint8_t> nv21(frameSize + frameSize / 2);

    for (int row = 0; row < height; row++) {
        memcpy(nv21.data() + row * width,
                reinterpret_cast<uint8_t*>(yPtr) + row * yRowStride,
                width);
    }
    env->ReleaseByteArrayElements(yData, yPtr, JNI_ABORT);

    jbyte *uvPtr = env->GetByteArrayElements(uvData, nullptr);
    uint8_t *vuDst = nv21.data() + frameSize;

    for (int row = 0; row < height / 2; row++) {
        for (int col = 0; col < width / 2; col++) {
            int uvIdx = row * uvRowStride + col * uvPixelStride;
            uint8_t u = reinterpret_cast<uint8_t*>(uvPtr)[uvIdx];
            uint8_t v = reinterpret_cast<uint8_t*>(uvPtr)[uvIdx + 1];
            *vuDst++ = v;
            *vuDst++ = u;
        }
    }
    env->ReleaseByteArrayElements(uvData, uvPtr, JNI_ABORT);

    Mat yuv(height + height / 2, width, CV_8UC1, nv21.data());
    Mat bgr;
    cvtColor(yuv, bgr, COLOR_YUV2BGR_NV21);
    return bgr.clone();
}


Mat preprocess(const Mat &bgr, Mat &outGray) {
    cvtColor(bgr, outGray, COLOR_BGR2GRAY);

    Ptr<CLAHE> clahe = createCLAHE(2.0, Size(8, 8));
    Mat claheGray;
    clahe->apply(outGray, claheGray);

    Mat blurred;
    GaussianBlur(claheGray, blurred, Size(5, 5), 0);

    Mat dummy;
    double otsuVal = threshold(blurred, dummy, 0, 255,
            THRESH_BINARY | THRESH_OTSU);
    double lo = max(otsuVal * 0.4, 30.0);
    double hi = otsuVal;

    LOGD("Otsu: %.1f  Canny: lo=%.1f hi=%.1f", otsuVal, lo, hi);

    Mat edges;
    Canny(blurred, edges, lo, hi);

    Mat kernel = getStructuringElement(MORPH_RECT, Size(3, 3));
    dilate(edges, edges, kernel, Point(-1, -1), 1);

    return edges;
}

struct QuadScore {
    vector<Point> poly;
    double score = -1.0;
};

bool validateQuad(const vector<Point> &poly,
        int imgW, int imgH,
        QuadScore &out) {

    if (poly.size() != 4) return false;

    double imageArea = imgW * imgH;
    double area      = contourArea(poly);
    double areaRatio = area / imageArea;

    if (areaRatio < MIN_AREA_RATIO) {
        LOGD("Reject: area ratio %.3f < %.3f", areaRatio, MIN_AREA_RATIO);
        return false;
    }
    if (areaRatio > MAX_AREA_RATIO) {
        LOGD("Reject: area ratio %.3f > %.3f", areaRatio, MAX_AREA_RATIO);
        return false;
    }

    vector<Point> hull;
    convexHull(poly, hull);
    double hullArea  = contourArea(hull);
    double convexity = area / (hullArea + 1e-6);
    if (convexity < CONVEXITY_THRESHOLD) {
        LOGD("Reject: convexity %.3f < %.3f", convexity, CONVEXITY_THRESHOLD);
        return false;
    }

    double angleScore = 0.0;
    for (int i = 0; i < 4; i++) {
        Point v1 = poly[i]            - poly[(i + 1) % 4];
        Point v2 = poly[(i + 2) % 4] - poly[(i + 1) % 4];
        double cosA  = v1.dot(v2) / (norm(v1) * norm(v2) + 1e-6);
        double angle = acos(clamp(cosA, -1.0, 1.0)) * 180.0 / CV_PI;
        if (angle < MIN_ANGLE_DEG || angle > MAX_ANGLE_DEG) {
            LOGD("Reject: angle %.1f out of [%.1f, %.1f]",
                    angle, MIN_ANGLE_DEG, MAX_ANGLE_DEG);
            return false;
        }
        angleScore += 1.0 - abs(angle - 90.0) / 90.0;
    }
    angleScore /= 4.0;

    double w = (norm(poly[0]-poly[1]) + norm(poly[2]-poly[3])) / 2.0;
    double h = (norm(poly[1]-poly[2]) + norm(poly[3]-poly[0])) / 2.0;
    if (w < 1.0 || h < 1.0) return false;
    double aspect = max(w, h) / min(w, h);
    if (aspect < MIN_ASPECT_RATIO || aspect > MAX_ASPECT_RATIO) {
        LOGD("Reject: aspect %.3f", aspect);
        return false;
    }

    out.score = areaRatio * 0.7 + angleScore * 0.3;
    out.poly  = poly;
    return true;
}

vector<Point2f> sortCornersCW(const vector<Point> &poly) {
    Point2f center(0, 0);
    for (const auto &p : poly) center += Point2f(p.x, p.y);
    center *= 0.25f;

    vector<pair<double, Point2f>> ap;
    for (const auto &p : poly)
        ap.push_back({atan2(p.y - center.y, p.x - center.x), Point2f(p.x, p.y)});
    sort(ap.begin(), ap.end(), [](const auto &a, const auto &b) {
        return a.first < b.first;
    });

    vector<Point2f> result;
    for (const auto &e : ap) result.push_back(e.second);
    return result;
}

void refineCorners(const Mat &gray, vector<Point2f> &corners) {
    if (corners.empty() || gray.empty()) return;
    TermCriteria tc(TermCriteria::EPS | TermCriteria::MAX_ITER, 30, 0.01);
    cornerSubPix(gray, corners, Size(5, 5), Size(-1, -1), tc);
}

QuadScore detectBestQuad(const Mat &edges, int origW, int origH) {
    vector<vector<Point>> contours;
    findContours(edges.clone(), contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

    LOGD("Total contours: %zu", contours.size());

    sort(contours.begin(), contours.end(),
            [](const vector<Point> &a, const vector<Point> &b) {
                return contourArea(a) > contourArea(b);
            });

    QuadScore best;

    int checkCount = min((int)contours.size(), 15);

    for (int ci = 0; ci < checkCount; ci++) {
        const auto &contour = contours[ci];
        double peri = arcLength(contour, true);

        if (peri < 50) continue;

        for (double epsFactor : {0.02, 0.03, 0.04, 0.05, 0.06}) {
            vector<Point> poly;
            approxPolyDP(contour, poly, epsFactor * peri, true);

            LOGD("Contour[%d] peri=%.0f eps=%.2f -> %zu points",
                    ci, peri, epsFactor, poly.size());

            if (poly.size() != 4) continue;

            QuadScore qs;
            if (validateQuad(poly, origW, origH, qs)) {
                if (qs.score > best.score) {
                    best = qs;
                    LOGD("New best! score=%.3f area=%.0f",
                            best.score, contourArea(poly));
                }
                break;
            }
        }

        if (best.score > 0.85) break;
    }

    return best;
}

vector<Point2f> smoothWithHistory(const vector<Point2f> &cur) {
    g_history.push_back(cur);
    if ((int)g_history.size() > SMOOTH_HISTORY)
        g_history.pop_front();

    vector<Point2f> smoothed(4, Point2f(0, 0));
    for (const auto &frame : g_history)
        for (int i = 0; i < 4; i++)
            smoothed[i] += frame[i];

    float n = static_cast<float>(g_history.size());
    for (auto &p : smoothed) p *= (1.0f / n);
    return smoothed;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_detectRectangles(
        JNIEnv *env,
        jobject,
        jbyteArray yData,
        jint yRowStride,
        jbyteArray uvData,
        jint uvRowStride,
        jint uvPixelStride,
        jint width,
        jint height
) {
    Mat bgr = yuvToBgr(env,
            yData, yRowStride,
            uvData, uvRowStride, uvPixelStride,
            width, height);

    if (bgr.empty()) {
        LOGW("BGR conversion failed");
        return env->NewFloatArray(0);
    }

    Mat gray;
    Mat edges = preprocess(bgr, gray);

    static const float SCALES[] = {1.0f, 0.75f, 0.5f};
    QuadScore best;

    for (float scale : SCALES) {
        Mat scaledEdges;
        int sw = static_cast<int>(width  * scale);
        int sh = static_cast<int>(height * scale);

        if (scale != 1.0f) resize(edges, scaledEdges, Size(sw, sh));
        else                scaledEdges = edges;

        QuadScore qs = detectBestQuad(scaledEdges, sw, sh);

        if (qs.score > best.score) {
            if (scale != 1.0f)
                for (auto &p : qs.poly) {
                    p.x = static_cast<int>(p.x / scale);
                    p.y = static_cast<int>(p.y / scale);
                }
            best = qs;
        }
        if (best.score > 0.85) break;
    }

    if (best.poly.empty()) {
        LOGW("No quad detected");
        g_history.clear();
        return env->NewFloatArray(0);
    }

    vector<Point2f> corners = sortCornersCW(best.poly);
    refineCorners(gray, corners);
    corners = smoothWithHistory(corners);

    jfloatArray result = env->NewFloatArray(9);
    jfloat pts[9] = {
            corners[0].x, corners[0].y,
            corners[1].x, corners[1].y,
            corners[2].x, corners[2].y,
            corners[3].x, corners[3].y,
            static_cast<jfloat>(best.score)
    };
    env->SetFloatArrayRegion(result, 0, 9, pts);

    LOGI("Detected! score=%.3f TL=(%.0f,%.0f)",best.score, corners[0].x, corners[0].y);
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_chs_yourdocscanner_OpenCVBridge_resetHistory(JNIEnv*, jobject) {
    g_history.clear();
}
