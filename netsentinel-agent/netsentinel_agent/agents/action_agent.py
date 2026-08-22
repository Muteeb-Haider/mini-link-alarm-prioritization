"""
Action Agent
------------
Final stage. Given a triage score + diagnosis, decides whether to:
  - AUTO_REMEDIATE : safe, low-risk, reversible action (simulated here as
                      a logged action rather than a real device call —
                      swap `_execute_action` for a real API/CLI call to
                      go from simulation to production)
  - ESCALATE        : drafts a structured incident ticket for a human
"""
import datetime


class ActionAgent:
    def act(self, triage: dict, diagnosis: dict) -> dict:
        auto_ok = diagnosis.get("auto_remediable", False)
        risk = diagnosis.get("risk", "high")
        service_affecting = triage["service_affecting"]

        # Never auto-remediate a service-affecting alarm, regardless of
        # what the runbook says — human approval required.
        if auto_ok and risk == "low" and not service_affecting:
            return self._execute_action(triage, diagnosis)
        else:
            return self._escalate(triage, diagnosis)

    def _execute_action(self, triage, diagnosis):
        return {
            "alarm_id": triage["alarm_id"],
            "decision": "AUTO_REMEDIATE",
            "action_taken": diagnosis["recommended_action"],
            "executed_at": datetime.datetime.now(datetime.timezone.utc).isoformat() + "Z",
        }

    def _escalate(self, triage, diagnosis):
        ticket = {
            "alarm_id": triage["alarm_id"],
            "node_id": triage["node_id"],
            "priority": "P1" if triage["service_affecting"] else "P3",
            "root_cause": diagnosis.get("root_cause"),
            "recommended_action": diagnosis.get(
                "recommended_action", "Manual investigation required."
            ),
            "created_at": datetime.datetime.now(datetime.timezone.utc).isoformat() + "Z",
        }
        return {
            "alarm_id": triage["alarm_id"],
            "decision": "ESCALATE",
            "ticket": ticket,
        }
