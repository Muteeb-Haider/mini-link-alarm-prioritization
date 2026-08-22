import json
from pathlib import Path
from netsentinel_agent.orchestrator import NetSentinelPipeline

DATA_DIR = Path(__file__).resolve().parent.parent / "data"


def load_alarms():
    with open(DATA_DIR / "sample_alarms.json") as f:
        return json.load(f)


def test_pipeline_runs_and_covers_all_alarms():
    alarms = load_alarms()
    pipeline = NetSentinelPipeline()
    results = pipeline.run(alarms)
    assert len(results) == len(alarms)


def test_critical_service_affecting_alarms_are_ranked_highest():
    alarms = load_alarms()
    pipeline = NetSentinelPipeline()
    results = pipeline.run(alarms)
    top = results[0]["triage"]
    assert top["severity"] == "Critical"
    assert top["service_affecting"] is True


def test_service_affecting_alarms_are_never_auto_remediated():
    alarms = load_alarms()
    pipeline = NetSentinelPipeline()
    results = pipeline.run(alarms)
    for r in results:
        if r["triage"]["service_affecting"]:
            assert r["outcome"]["decision"] == "ESCALATE"


def test_low_risk_non_service_affecting_alarms_are_auto_remediated():
    alarms = load_alarms()
    pipeline = NetSentinelPipeline()
    results = pipeline.run(alarms)
    fan_variance = next(r for r in results if r["triage"]["alarm_id"] == "ALM-5112")
    assert fan_variance["outcome"]["decision"] == "AUTO_REMEDIATE"


def test_diagnosis_confidence_is_reasonable_for_known_patterns():
    alarms = load_alarms()
    pipeline = NetSentinelPipeline()
    results = pipeline.run(alarms)
    for r in results:
        assert r["diagnosis"]["confidence"] >= 0.05
