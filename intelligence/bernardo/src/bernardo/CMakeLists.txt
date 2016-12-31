ADD_DEFINITIONS(-std=c++1y)

include_directories(.)
include_directories(..)

file(GLOB src *.cpp)
file(GLOB headers *.hpp)

add_executable(
    bernardo
    ${src})

target_link_libraries(
    bernardo
    bernardo_util
    ${Boost_LIBRARIES}
    ${MISC_LIBRARIES})

add_dependencies(
    bernardo 
    bernardo_util)

install(TARGETS bernardo DESTINATION bin)
