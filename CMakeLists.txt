cmake_minimum_required(VERSION 2.8.11)
project(henhouse)

set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake/)

include_directories(.)
include_directories(..)

#setup boost
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost COMPONENTS system program_options filesystem regex iostreams thread serialization REQUIRED)
link_directories(${Boost_LIBRARY_DIRS})
include_directories(
    ${Boost_INCLUDE_DIRS})
include_directories(/usr/include/botan-1.10/)

set(MISC_LIBRARIES 
    librt.so 
    libproxygenhttpserver.a 
    libproxygenlib.a
    libz.a
    libwangle.a 
    libfolly.a 
    libevent.a
    libpthread.so 
    ssl 
    crypto 
    glog 
    gflags
    botan-1.10 
    libdouble-conversion.a
    pqxx)

add_subdirectory(src)
