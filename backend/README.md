# AImong Backend

Spring Boot backend for AImong.

## Profiles

Choose the runtime YAML in `.env`.

Local:

```properties
SPRING_PROFILES_ACTIVE=local
LOCAL_DB_URL=jdbc:postgresql://localhost:54329/aimong_local
LOCAL_DB_USERNAME=aimong
LOCAL_DB_PASSWORD=aimong

TEST_DB_URL=jdbc:postgresql://localhost:54329/aimong_test
TEST_DB_USERNAME=aimong
TEST_DB_PASSWORD=aimong
```

Production:

```properties
SPRING_PROFILES_ACTIVE=prod
SUPABASE_DB_URL=jdbc:postgresql://db.your-project.supabase.co:5432/postgres
SUPABASE_DB_USERNAME=postgres
SUPABASE_DB_PASSWORD=your-password
```

## Local Development

Start local dependencies:

```powershell
docker compose -f docker-compose.local.yml up -d
```

Run tests:

```powershell
.\gradlew.bat test
```

Run the app:

```powershell
.\gradlew.bat bootRun
```

Reset local data:

```powershell
docker compose -f docker-compose.local.yml down -v
```

## Document Policy

- Public repository docs belong in `README.md` or future tracked `docs/`.
- Private notes, raw references, and working drafts belong in `private-docs/`.
- Generated reports and question-bank artifacts belong in `_generated/`.

## Question Bank Semantic Audit

Install Python dependencies in an environment that has Python 3.10+:

```powershell
python -m pip install openai numpy
```

Run a dry-run without API calls:

```powershell
python scripts/audit_question_bank_semantic_similarity.py --input _generated/question-bank/question-bank-1056-starlevel-ultra-diverse-patched.json --model text-embedding-3-large --dry-run
```

Run the OpenAI Embeddings audit:

```powershell
$env:OPENAI_API_KEY="sk-..."
python scripts/audit_question_bank_semantic_similarity.py --input _generated/question-bank/question-bank-1056-starlevel-ultra-diverse-patched.json --model text-embedding-3-large
```

From the repository root, the same command is:

```powershell
python backend/scripts/audit_question_bank_semantic_similarity.py --input backend/_generated/question-bank/question-bank-1056-starlevel-ultra-diverse-patched.json --model text-embedding-3-large
```

Generated outputs:

- `_generated/question-bank/question-bank-1056-openai-semantic-similarity-report.md`
- `_generated/question-bank/question-bank-1056-openai-semantic-similarity-pairs.csv`
- `_generated/question-bank/question-bank-1056-openai-semantic-similarity-clusters.csv`
- `_generated/question-bank/question-bank-1056-openai-semantic-similarity-pairs.json`
- `_generated/question-bank/question-bank-1056-openai-semantic-similarity-clusters.json`
- `_generated/question-bank/question-bank-1056-openai-semantic-judge-candidates.jsonl`

Embedding cache:

- `.cache/question-bank-openai-embeddings.jsonl`
