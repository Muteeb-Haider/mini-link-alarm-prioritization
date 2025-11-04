#include "Prioritizer.hpp"
#include "Alarm.hpp"
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <chrono>
#include <ctime>
#include <nlohmann/json.hpp>

using json = nlohmann::json;
using namespace std;

static chrono::system_clock::time_point parseIso(const string& s){
    std::tm tm{};
    string ss = s;
    // Simple Zulu trimming
    if (!ss.empty() && (ss.back()=='Z' || ss.back()=='z')) ss.pop_back();
    strptime(ss.c_str(), "%Y-%m-%dT%H:%M:%S", &tm);
    
    // Portable timegm alternative
    #ifdef _WIN32
        time_t tt = _mkgmtime(&tm);
    #else
        time_t tt = timegm(&tm);
    #endif
    
    return chrono::system_clock::from_time_t(tt);
}

static Alarm alarmFromJson(const json& j){
    Alarm a;
    a.id = j.value("id","");
    a.nodeId = j.value("nodeId","");
    a.severity = j.value("severity","Info");
    a.firstSeen = parseIso(j.value("firstSeen","1970-01-01T00:00:00Z"));
    a.lastSeen = parseIso(j.value("lastSeen","1970-01-01T00:00:00Z"));
    a.occurrencesPerHour = j.value("occurrencesPerHour", 0.0);
    a.affectedLinks = j.value("affectedLinks", 0);
    a.trafficImpactPct = j.value("trafficImpactPct", 0.0);
    a.serviceAffecting = j.value("serviceAffecting", false);
    a.description = j.value("description","");
    return a;
}

static json rankedToJson(const RankedAlarm& r){
    json j;
    j["id"] = r.alarm.id;
    j["nodeId"] = r.alarm.nodeId;
    j["severity"] = r.alarm.severity;
    j["score"] = r.score;
    j["rank"] = r.rank;
    j["reason"] = r.reason;
    return j;
}

static ScoringConfig loadConfig(const string& path){
    ifstream in(path);
    if (!in) throw runtime_error("Failed to open config: " + path);
    json j; in >> j;
    ScoringConfig c;
    for (auto& [k,v]: j["severityWeights"].items()) c.severityWeights[k] = v.get<double>();
    c.alphaFrequency = j.value("alphaFrequency", c.alphaFrequency);
    c.betaImpact = j.value("betaImpact", c.betaImpact);
    c.gammaServiceAffectingBonus = j.value("gammaServiceAffectingBonus", c.gammaServiceAffectingBonus);
    c.recencyHalfLifeHours = j.value("recencyHalfLifeHours", c.recencyHalfLifeHours);
    c.norm.maxOccurrencesPerHour = j["norm"].value("maxOccurrencesPerHour", c.norm.maxOccurrencesPerHour);
    c.norm.maxAffectedLinks = j["norm"].value("maxAffectedLinks", c.norm.maxAffectedLinks);
    c.norm.maxTrafficImpactPct = j["norm"].value("maxTrafficImpactPct", c.norm.maxTrafficImpactPct);
    return c;
}

int main(int argc, char** argv){
    string input="data/sample_alarms.json";
    string output="";
    string config="config/scoring.json";
    int topN = -1;
    string format="table"; // or json

    for (int i=1;i<argc;++i){
        string a = argv[i];
        if (a=="--input" && i+1<argc) input = argv[++i];
        else if (a=="--output" && i+1<argc) output = argv[++i];
        else if (a=="--config" && i+1<argc) config = argv[++i];
        else if (a=="--top" && i+1<argc) topN = stoi(argv[++i]);
        else if (a=="--format" && i+1<argc) format = argv[++i];
        else if (a=="--help"){
            cout << "Usage: alarm_cli --input <file.json> [--output <out.json>] [--config <scoring.json>] [--top N] [--format table|json]\n";
            return 0;
        }
    }

    // Load config
    ScoringConfig cfg;
    try {
        cfg = loadConfig(config);
    } catch (const exception& e) {
        cerr << "Failed to load config: " << e.what() << "\n";
        return 4;
    }
    Prioritizer p(cfg);

    // Load alarms
    ifstream in(input);
    if (!in){ cerr << "Cannot open input: " << input << "\n"; return 2; }
    json j;
    try {
        in >> j;
    } catch (const json::exception& e) {
        cerr << "Failed to parse JSON: " << e.what() << "\n";
        return 3;
    }
    vector<Alarm> alarms;
    for (auto& e: j) alarms.push_back(alarmFromJson(e));

    auto now = chrono::system_clock::now();
    auto ranked = p.prioritize(alarms, now, topN);

    if (format=="json"){
        json out = json::array();
        for (auto& r: ranked) out.push_back(rankedToJson(r));
        if (!output.empty()){
            ofstream o(output); o << out.dump(2) << "\n";
        } else {
            cout << out.dump(2) << "\n";
        }
    } else {
        // Table print
        cout << "RANK  SCORE    ID         NODE          SEV     REASON\n";
        for (auto& r: ranked){
            cout << r.rank << "     " << r.score << "  " << r.alarm.id << "  " << r.alarm.nodeId
                 << "  " << r.alarm.severity << "  " << r.reason << "\n";
        }
    }
    return 0;
}
