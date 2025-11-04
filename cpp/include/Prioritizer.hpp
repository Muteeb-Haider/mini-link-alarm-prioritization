#pragma once
#include "Alarm.hpp"
#include <vector>
#include <string>
#include <unordered_map>

struct ScoringConfig {
    std::unordered_map<std::string, double> severityWeights;
    double alphaFrequency = 10.0;
    double betaImpact = 1.0;
    double gammaServiceAffectingBonus = 10.0;
    double recencyHalfLifeHours = 6.0;
    struct Norm {
        double maxOccurrencesPerHour = 20.0;
        int maxAffectedLinks = 10;
        double maxTrafficImpactPct = 100.0;
    } norm;
};

class Prioritizer {
public:
    explicit Prioritizer(const ScoringConfig& cfg);
    RankedAlarm score(const Alarm& a, std::chrono::system_clock::time_point now) const;
    std::vector<RankedAlarm> prioritize(const std::vector<Alarm>& alarms, std::chrono::system_clock::time_point now, int topN = -1) const;
private:
    ScoringConfig cfg_;
};
