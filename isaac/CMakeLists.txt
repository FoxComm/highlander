cmake_minimum_required(VERSION 2.8.11)
project(henhouse)

set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake/)

include_directories(.)
include_directories(..)
include(FindPkgConfig)

#setup boost
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost COMPONENTS system program_options filesystem regex iostreams thread serialization context REQUIRED)
link_directories(${Boost_LIBRARY_DIRS})
include_directories(
    ${Boost_INCLUDE_DIRS})

pkg_check_modules (DEPS REQUIRED botan-1.10 libpqxx)
include_directories(${DEPS_INCLUDE_DIRS})

set(MISC_LIBRARIES
    librt.so
    libproxygenhttpserver.a
    libproxygenlib.a
    libz.a
    libwangle.a
    libfolly.so
    libevent.a
    libpthread.so
    ssl
    crypto
    glog
    gflags
    botan-1.10
    libdouble-conversion.a
    pqxx
    dl)

add_subdirectory(src)
