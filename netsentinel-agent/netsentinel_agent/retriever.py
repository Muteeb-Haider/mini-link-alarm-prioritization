"""
Retrieval layer: embeds the runbook knowledge base with TF-IDF and finds
the closest matching runbook for a given alarm description. This is the
"RAG" piece of the pipeline — swap this for a real embedding model /
vector DB (e.g. sentence-transformers + FAISS) without touching the
rest of the pipeline; the interface (retrieve()) stays the same.
"""
import json
from pathlib import Path
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

DATA_DIR = Path(__file__).resolve().parent.parent / "data"


class RunbookRetriever:
    def __init__(self, runbooks_path: Path = DATA_DIR / "runbooks.json"):
        with open(runbooks_path) as f:
            self.runbooks = json.load(f)

        corpus = [f"{rb['title']} {rb['keywords']}" for rb in self.runbooks]
        self.vectorizer = TfidfVectorizer(stop_words="english")
        self.matrix = self.vectorizer.fit_transform(corpus)

    def retrieve(self, alarm_description: str, top_k: int = 1):
        """Return the top_k most relevant runbooks for an alarm description,
        each annotated with a similarity score."""
        query_vec = self.vectorizer.transform([alarm_description])
        sims = cosine_similarity(query_vec, self.matrix).flatten()
        ranked_idx = sims.argsort()[::-1][:top_k]

        results = []
        for i in ranked_idx:
            rb = dict(self.runbooks[i])
            rb["similarity"] = round(float(sims[i]), 3)
            results.append(rb)
        return results
