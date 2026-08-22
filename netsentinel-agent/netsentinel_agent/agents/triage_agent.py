"""
Triage Agent
------------
First stage of the pipeline. Scores each alarm using the same weighted
formula as the original C++ Prioritizer (severity weight + frequency +
traffic impact + service-affecting bonus), so results stay consistent
with the existing mini-link-alarm-prioritization engine. Output feeds
the Diagnosis Agent.
"""
import json
from pathlib import Path

CONFIG_PATH = Path(__file__).resolve().parents[2] / "config" / "scoring.json"


class TriageAgent:
    def __init__(self, config_path: Path = CONFIG_PATH):
        with open(config_path) as f:
            self.cfg = json.load(f)

    def score(self, alarm: dict) -> dict:
        cfg = self.cfg
        severity_score = cfg["severityWeights"].get(alarm["severity"], 0.0)

        freq_norm = min(
            alarm["occurrencesPerHour"] / cfg["norm"]["maxOccurrencesPerHour"], 1.0
        )
        impact_norm = min(
            alarm["trafficImpactPct"] / cfg["norm"]["maxTrafficImpactPct"], 1.0
        )

        frequency_score = cfg["alphaFrequency"] * freq_norm
        impact_score = cfg["betaImpact"] * severity_score * impact_norm
        service_bonus = (
            cfg["gammaServiceAffectingBonus"] if alarm["serviceAffecting"] else 0.0
        )

        total = severity_score + frequency_score + impact_score + service_bonus

        return {
            "alarm_id": alarm["id"],
            "node_id": alarm["nodeId"],
            "score": round(total, 2),
            "severity": alarm["severity"],
            "service_affecting": alarm["serviceAffecting"],
        }

    def triage(self, alarms: list) -> list:
        scored = [self.score(a) for a in alarms]
        scored.sort(key=lambda x: x["score"], reverse=True)
        for rank, item in enumerate(scored, start=1):
            item["rank"] = rank
        return scored
