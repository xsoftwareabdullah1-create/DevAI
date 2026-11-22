cmake_minimum_required(VERSION 3.22.1)
project(devai LANGUAGES CXX)

add_library(devai SHARED
    native_bridge.cpp
    tokenizer.cpp
    transformer.cpp
)

find_library(log-lib log)
target_link_libraries(devai ${log-lib})
