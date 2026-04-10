@echo off
chcp 65001 > nul
cd /d "C:\Users\hwkim\DEV\AImong"

echo [1/4] 현재 브랜치 확인...
git branch

echo.
echo [2/4] 변경 파일 스테이징...
git add backend/src/main/java/com/aimong/backend/global/
git add backend/src/main/java/com/aimong/backend/domain/
git add backend/src/main/resources/application.yaml

echo.
echo [3/4] 커밋...
git commit -m "feat(backend): ERD v1.1 기반 JPA Entity 클래스 및 Supabase DB 연결 설정"

echo.
echo [4/4] Push...
git push origin feature/entity-db-setup

echo.
echo 완료! GitHub에서 PR 생성하세요:
echo https://github.com/KDUcapstone/AImong/compare/dev...feature/entity-db-setup?expand=1
pause
