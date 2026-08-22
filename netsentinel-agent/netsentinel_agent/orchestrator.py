"""
NetSentinel Orchestrator
-------------------------
Wires the three agents together into one incident-response pipeline:

    Alarm -> TriageAgent -> DiagnosisAgent (RAG) -> ActionAgent -> Outcome

Run directly:  python -m netsentinel_agent.orchestrator
"""
import json
from pathlib import Path

from netsentinel_agent.agents.triage_agent import TriageAgent
from netsentinel_agent.agents.diagnosis_agent import DiagnosisAgent
from netsentinel_agent.agents.action_agent import ActionAgent
from netsentinel_agent.retriever import RunbookRetriever

DATA_DIR = Path(__file__).resolve().parent.parent / "data"


class NetSentinelPipeline:
    def __init__(self):
        self.triage_agent = TriageAgent()
        self.diagnosis_agent = DiagnosisAgent(RunbookRetriever())
        self.action_agent = ActionAgent()

    def run(self, alarms: list) -> list:
        triaged = self.triage_agent.triage(alarms)
        alarms_by_id = {a["id"]: a for a in alarms}

        results = []
        for t in triaged:
            alarm = alarms_by_id[t["alarm_id"]]
            diagnosis = self.diagnosis_agent.diagnose(alarm)
            outcome = self.action_agent.act(t, diagnosis)

            results.append(
                {
                    "triage": t,
                    "diagnosis": diagnosis,
                    "outcome": outcome,
                }
            )
        return results


def main():
    with open(DATA_DIR / "sample_alarms.json") as f:
        alarms = json.load(f)

    pipeline = NetSentinelPipeline()
    results = pipeline.run(alarms)

    print(f"\n{'='*70}\nNetSentinel — Incident Response Run ({len(alarms)} alarms)\n{'='*70}\n")
    for r in results:
        t, d, o = r["triage"], r["diagnosis"], r["outcome"]
        print(f"[Rank {t['rank']}] {t['alarm_id']} ({t['node_id']}) "
              f"— severity={t['severity']}, score={t['score']}")
        print(f"   Diagnosis : {d['root_cause']} (confidence={d.get('confidence', 0)})")
        print(f"   Decision  : {o['decision']}")
        if o["decision"] == "AUTO_REMEDIATE":
            print(f"   Action    : {o['action_taken']}")
        else:
            print(f"   Ticket    : priority={o['ticket']['priority']}, "
                  f"action={o['ticket']['recommended_action']}")
        print()

    out_path = DATA_DIR / "pipeline_run_output.json"
    with open(out_path, "w") as f:
        json.dump(results, f, indent=2)
    print(f"Full reasoning trace written to {out_path}")

    return results


if __name__ == "__main__":
    main()
