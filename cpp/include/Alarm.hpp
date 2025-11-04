#pragma once
#include <string>
#include <vector>
#include <chrono>

struct Alarm {
    std::string id;
    std::string nodeId;
    std::string severity; // Critical, Major, Minor, Warning, Info
    std::chrono::system_clock::time_point firstSeen;
    std::chrono::system_clock::time_point lastSeen;
    double occurrencesPerHour = 0.0;
    int affectedLinks = 0;
    double trafficImpactPct = 0.0; // 0..100
    bool serviceAffecting = false;
    std::string description;
};

struct RankedAlarm {
    Alarm alarm;
    double score = 0.0;
    int rank = 0;
    std::string reason;
};
