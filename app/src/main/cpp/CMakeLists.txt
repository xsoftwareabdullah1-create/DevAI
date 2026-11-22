cmake_minimum_required(VERSION 3.22.1)
project(devai LANGUAGES CXX)

set(LITERTLM_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/litertlm")
set(LITERTLM_INCLUDE "${LITERTLM_ROOT}/include")
set(LITERTLM_LIBRARY "${LITERTLM_ROOT}/lib/arm64-v8a/libLiteRtLm.so")

if(NOT EXISTS "${LITERTLM_INCLUDE}/c/engine.h" OR NOT EXISTS "${LITERTLM_LIBRARY}")
    message(FATAL_ERROR "LiteRT-LM SDK is missing. Run scripts/prepare_litertlm_android.sh before building.")
endif()

add_library(litertlm SHARED IMPORTED GLOBAL)
set_target_properties(litertlm PROPERTIES\n    IMPORTED_LOCATION "${LITERTLM_LIBRARY}"\n    IMPORTED_SONAME "libLiteRtLm.so"\n)

add_library(devai SHARED
    native_bridge.cpp
    tokenizer.cpp
    transformer.cpp
)

target_include_directories(devai PRIVATE "${LITERTLM_INCLUDE}")
find_library(log-lib log)
target_link_libraries(devai PRIVATE ${log-lib} litertlm)
