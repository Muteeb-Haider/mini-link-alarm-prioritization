cmake_minimum_required(VERSION 3.16)
project(alarm_prioritizer CXX)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# nlohmann::json single-header
include(FetchContent)
FetchContent_Declare(
  json
  GIT_REPOSITORY https://github.com/nlohmann/json.git
  GIT_TAG v3.11.3
)
FetchContent_MakeAvailable(json)

add_library(alarmlib
    src/Alarm.cpp
    src/Prioritizer.cpp
)
target_include_directories(alarmlib PUBLIC include)

add_executable(alarm_cli src/main.cpp)
target_link_libraries(alarm_cli PRIVATE alarmlib nlohmann_json::nlohmann_json)

# Install target (optional)
install(TARGETS alarm_cli DESTINATION bin)
