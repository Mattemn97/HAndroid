cmake_minimum_required(VERSION 3.22)
project(handroid C)

# Prodotto da scripts/prepara-python-android.sh
set(PYTHON_PREFIX ${CMAKE_SOURCE_DIR}/../../../python/prefix)
set(JNI_LIBS ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

add_library(handroid SHARED handroid.c)
target_include_directories(handroid PRIVATE ${PYTHON_PREFIX}/include/python3.14)
target_link_libraries(handroid ${JNI_LIBS}/libpython3.14.so log)
