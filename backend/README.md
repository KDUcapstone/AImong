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
