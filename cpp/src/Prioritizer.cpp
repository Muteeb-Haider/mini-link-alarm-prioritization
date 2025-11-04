#include "Prioritizer.hpp"
#include <algorithm>
#include <cmath>
#include <sstream>

using namespace std;

static double clamp01(double v){ return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v); }

Prioritizer::Prioritizer(const ScoringConfig& cfg): cfg_(cfg) {}

RankedAlarm Prioritizer::score(const Alarm& a, chrono::system_clock::time_point now) const {
    double sev = 0.0;
    auto it = cfg_.severityWeights.find(a.severity);
    if (it != cfg_.severityWeights.end()) sev = it->second;

    // Normalized components
    double freqNorm = clamp01(a.occurrencesPerHour / max(1e-9, cfg_.norm.maxOccurrencesPerHour));
    double linkNorm = clamp01(static_cast<double>(a.affectedLinks) / max(1, cfg_.norm.maxAffectedLinks));
    double impactNorm = clamp01(a.trafficImpactPct / max(1e-9, cfg_.norm.maxTrafficImpactPct));

    // Recency via exponential decay by age since lastSeen
    auto age = chrono::duration_cast<chrono::minutes>(now - a.lastSeen).count() / 60.0; // hours
    double hl = max(1e-6, cfg_.recencyHalfLifeHours);
    double recency = pow(0.5, max(0.0, age) / hl); // 1.0 if just now, decays with age

    // Composite score
    double score = sev
                 + cfg_.alphaFrequency * freqNorm
                 + cfg_.betaImpact * (0.6 * impactNorm + 0.4 * linkNorm) * 100.0 * recency
                 + (a.serviceAffecting ? cfg_.gammaServiceAffectingBonus : 0.0);

    // Reason string
    ostringstream rs;
    rs << (a.serviceAffecting ? "Service affecting; " : "");
    if (sev >= 100.0) rs << "Critical severity; ";
    else if (sev >= 70.0) rs << "Major severity; ";
    else if (sev >= 40.0) rs << "Minor/Warning; ";
    if (a.trafficImpactPct > 0) rs << "Traffic impact " << a.trafficImpactPct << "%; ";
    if (a.affectedLinks > 0) rs << a.affectedLinks << " links affected; ";
    if (a.occurrencesPerHour > 0) rs << a.occurrencesPerHour << " occ/hr; ";
    rs << "Recency factor " << recency;

    RankedAlarm r{a, score, 0, rs.str()};
    return r;
}

vector<RankedAlarm> Prioritizer::prioritize(const vector<Alarm>& alarms, chrono::system_clock::time_point now, int topN) const {
    vector<RankedAlarm> ranked;
    ranked.reserve(alarms.size());
    for (const auto& a : alarms) ranked.push_back(score(a, now));
    sort(ranked.begin(), ranked.end(), [](const RankedAlarm& x, const RankedAlarm& y){
        if (x.score == y.score) return x.alarm.severity < y.alarm.severity;
        return x.score > y.score;
    });
    for (size_t i=0;i<ranked.size();++i) ranked[i].rank = static_cast<int>(i+1);
    if (topN > 0 && static_cast<size_t>(topN) < ranked.size()) ranked.resize(topN);
    return ranked;
}
