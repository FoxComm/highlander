ADD_DEFINITIONS(-std=c++17)

include_directories(.)
include_directories(..)

file(GLOB src *.cpp)

add_library(bernardo_util STATIC ${src})
