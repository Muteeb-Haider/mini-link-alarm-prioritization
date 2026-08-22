# NetSentinel — Agentic Incident Response Layer

An autonomous multi-agent system that sits on top of the [MINI-LINK Alarm
Prioritization Engine](../) and turns raw network alarms into triaged,
diagnosed, and (where safe) auto-remediated incidents — with a full
reasoning trace instead of a black-box decision.

This extends the original C++ scoring engine with an AI reasoning layer:
the same alarm data that used to just get *ranked* now gets *diagnosed*
and *acted on* by three cooperating agents.

## Why this exists

Traditional alarm dashboards tell an on-call engineer *what* broke, but
not *why*, and definitely won't touch anything automatically. NetSentinel
closes that gap for the low-risk, well-understood cases while staying
conservative on anything service-affecting.

## Architecture

```
   Alarm data --> Triage Agent --> Diagnosis Agent (RAG) --> Action Agent
                  (scores +         (retrieves closest        (AUTO_REMEDIATE
                   ranks, reuses    matching runbook via       or ESCALATE
                   the original     TF-IDF / cosine            with a drafted
                   C++ scoring      similarity over a          ticket)
                   formula)         runbook knowledge base)
```

Every alarm produces a full reasoning trace (`data/pipeline_run_output.json`)
showing the triage score, the retrieved runbook + confidence, and the
final decision — so the output is auditable, not a black box.

**Safety rule:** the Action Agent never auto-remediates a
service-affecting alarm, regardless of what the runbook recommends —
those always route to a human via a drafted ticket.

## Real output from a pipeline run

This is the actual output of `python -m netsentinel_agent.orchestrator`
against the sample alarm set (not mocked):

| Rank | Alarm | Severity | Score | Diagnosis (confidence) | Decision |
|---|---|---|---|---|---|
| 1 | ALM-4007 | Critical | 163.6 | Radio interference (0.938) | **ESCALATE** — P1 ticket |
| 2 | ALM-1001 | Critical | 155.25 | Physical layer / link down (0.754) | **ESCALATE** — P1 ticket |
| 3 | ALM-2034 | Major | 73.8 | SFP Rx power degrading (0.949) | **AUTO_REMEDIATE** |
| 4 | ALM-3055 | Minor | 46.0 | Thermal spike (0.905) | **AUTO_REMEDIATE** |
| 5 | ALM-5112 | Warning | 20.15 | Fan speed variance (0.917) | **AUTO_REMEDIATE** |

Full reasoning trace for every field above: [`data/pipeline_run_output.json`](data/pipeline_run_output.json)

Test suite: 5/5 passing — `PYTHONPATH=. pytest tests/ -v`

## Run it yourself

```bash
pip install -r requirements.txt
PYTHONPATH=. python -m netsentinel_agent.orchestrator
PYTHONPATH=. python -m pytest tests/ -v
```

## What's real vs. simulated (honest disclosure)

- **Real:** the scoring math, the RAG retrieval and similarity scoring,
  the escalate/auto-remediate decision logic, and the test suite — all
  execute against real code paths, and the table above is a real run,
  not hand-written.
- **Simulated:** `_execute_action()` in `action_agent.py` logs the action
  instead of calling a live network device — swap that one function for
  a real device API/CLI call to go from portfolio project to production.
- **Runbook knowledge base:** currently a small hand-written set of 5
  runbooks in `data/runbooks.json`. Swap the TF-IDF retriever for a real
  embedding model + vector DB (sentence-transformers + FAISS/Chroma) and
  it scales to hundreds of runbooks without changing the pipeline.

## Extending this

- Swap `TfidfVectorizer` for a proper embedding model once you have an
  API key (Anthropic/OpenAI) or a local model — the `RunbookRetriever`
  interface (`retrieve()`) doesn't need to change.
- Add a fourth **Monitoring Agent** that watches whether auto-remediated
  alarms actually clear, and escalates automatically if they don't.
- Feed the reasoning trace into the existing React dashboard as a live
  panel next to each alarm.

## Tech stack

Python, scikit-learn (TF-IDF/RAG retrieval), pytest — designed to plug
into the existing `mini-link-alarm-prioritization` C++/React/Docker
stack without touching the original scoring engine.
