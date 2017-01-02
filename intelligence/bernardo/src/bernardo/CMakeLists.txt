ADD_DEFINITIONS(-std=c++17)

include_directories(.)
include_directories(..)

file(GLOB src *.cpp)
file(GLOB headers *.hpp)

add_executable(
    bernardo
    ${src})

target_link_libraries(
    bernardo
    bernardo_cluster
    bernardo_service
    bernardo_util
    ${Boost_LIBRARIES}
    ${MISC_LIBRARIES})

add_dependencies(
    bernardo 
    bernardo_cluster
    bernardo_service
    bernardo_util)

install(TARGETS bernardo DESTINATION bin)
