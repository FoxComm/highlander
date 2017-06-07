ADD_DEFINITIONS(-std=c++14)

include_directories(.)
include_directories(..)

file(GLOB src *.cpp)
file(GLOB headers *.hpp)

add_executable(
    isaac
    ${src}
    ${moc_resources})

target_link_libraries(
    isaac
    isaac_service
    isaac_db
    isaac_util
    ${MISC_LIBRARIES}
    ${Boost_LIBRARIES})


add_dependencies(
    isaac 
    isaac_service
    isaac_db
    isaac_util)

install(TARGETS isaac DESTINATION bin)
