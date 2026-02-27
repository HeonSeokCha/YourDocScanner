cmake_minimum_required(VERSION 3.22.1)
project("yourdocscanner")

set(OpenCV_DIR "${CMAKE_SOURCE_DIR}/../../../../sdk/native/jni")

find_package(OpenCV REQUIRED)

add_library(
    native-lib
    SHARED
    native-lib.cpp
)

target_link_libraries(
    native-lib
    ${OpenCV_LIBS}
    android
    log
)