cmake_minimum_required(VERSION 3.22.1)
project(specialgram_core LANGUAGES CXX)
add_library(specialgram_core SHARED
    src/main/cpp/specialgram_jni.cpp
    ../../shared/cpp/SpecialgramCore.cpp
    ../../shared/cpp/SpecialgramCoreC.cpp
)
target_include_directories(specialgram_core PRIVATE ../../shared/cpp)
target_compile_features(specialgram_core PRIVATE cxx_std_17)
target_compile_options(specialgram_core PRIVATE -Wall -Wextra -Werror)
