cmake_minimum_required(VERSION 3.22.1)
project(devai LANGUAGES CXX)

set(LITERTLM_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/third_party/litertlm")
set(LITERTLM_INCLUDE "${LITERTLM_ROOT}/include")
set(LITERTLM_LIBRARY "${LITERTLM_ROOT}/lib/arm64-v8a/libLiteRtLm.so")

if(NOT EXISTS "${LITERTLM_INCLUDE}/c/engine.h" OR NOT EXISTS "${LITERTLM_LIBRARY}")
    message(FATAL_ERROR "LiteRT-LM SDK is missing. Run scripts/prepare_litertlm_android.sh before building.")
endif()

# The upstream Android .so has no DT_SONAME. Tell CMake to link it by its
# library name rather than embedding the CI machine's absolute file path in
# libdevai.so's DT_NEEDED entry. The runtime copy is packaged from jniLibs.
add_library(litertlm SHARED IMPORTED GLOBAL)
set_target_properties(litertlm PROPERTIES
    IMPORTED_LOCATION "${LITERTLM_LIBRARY}"
    IMPORTED_NO_SONAME TRUE
)

add_library(devai SHARED
    native_bridge.cpp
    tokenizer.cpp
    transformer.cpp
)

target_include_directories(devai PRIVATE "${LITERTLM_INCLUDE}")
find_library(log-lib log)
target_link_libraries(devai PRIVATE ${log-lib} litertlm)
