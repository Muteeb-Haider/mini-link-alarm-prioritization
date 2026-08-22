"""
Diagnosis Agent
---------------
Takes a triaged alarm, retrieves the closest matching runbook via RAG,
and produces a root-cause explanation with a confidence score. This is
the "reasoning trace" that gets shown in the dashboard so the decision
isn't a black box.
"""
from netsentinel_agent.retriever import RunbookRetriever


class DiagnosisAgent:
    def __init__(self, retriever: RunbookRetriever = None):
        self.retriever = retriever or RunbookRetriever()

    def diagnose(self, alarm: dict) -> dict:
        matches = self.retriever.retrieve(alarm["description"], top_k=1)
        best = matches[0] if matches else None

        if not best or best["similarity"] < 0.05:
            return {
                "alarm_id": alarm["id"],
                "matched_runbook": None,
                "root_cause": "No matching runbook found — novel alarm pattern.",
                "confidence": 0.0,
            }

        return {
            "alarm_id": alarm["id"],
            "matched_runbook": best["id"],
            "root_cause": best["root_cause"],
            "confidence": best["similarity"],
            "recommended_action": best["recommended_action"],
            "auto_remediable": best["auto_remediable"],
            "risk": best["risk"],
        }
