cmake_minimum_required(VERSION 3.22)
project(handroid C)

# Preparato da scripts/prepara-python.sh
set(PY_PREFIX ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/python/prefix)

add_library(handroid SHARED launcher.c)
target_include_directories(handroid PRIVATE ${PY_PREFIX}/include/python3.14)
target_link_libraries(handroid ${PY_PREFIX}/lib/libpython3.14.so log)
