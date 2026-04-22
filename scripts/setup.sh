#!/bin/bash

set -e

COMPOSE_FILE="docker-compose/docker-compose.yml"

echo "🚀 Notio 개발 환경 설정 시작..."

# 1. 환경 변수 파일 생성
if [ ! -f .env ]; then
    echo "📝 .env 파일 생성 중..."
    cp .env.example .env
    echo "✅ .env 파일이 생성되었습니다. 필요한 값을 수정해주세요."
fi

# 2. Docker Compose 실행
echo "🐳 Docker Compose 서비스 시작 중..."
docker compose -f "$COMPOSE_FILE" up -d postgres redis ollama

# 3. PostgreSQL 연결 대기
echo "⏳ PostgreSQL 준비 대기 중..."
until docker exec notio-postgres pg_isready -U notio > /dev/null 2>&1; do
    sleep 1
done
echo "✅ PostgreSQL 준비 완료"

# 4. Ollama 모델 다운로드
echo "🤖 Ollama 모델 다운로드 중..."
docker exec notio-ollama ollama pull llama3.2:3b
docker exec notio-ollama ollama pull nomic-embed-text
echo "✅ Ollama 모델 다운로드 완료"

# 5. Backend 의존성 설치 (Gradle Wrapper 다운로드)
if [ -d "backend" ]; then
    echo "📦 Backend Gradle Wrapper 다운로드 중..."
    cd backend
    if [ ! -f gradlew ]; then
        gradle wrapper --gradle-version 8.12
    fi
    ./gradlew --version
    cd ..
    echo "✅ Backend Gradle 설정 완료"
fi

# 6. Frontend 의존성 설치
if [ -d "frontend" ]; then
    echo "📦 Frontend 의존성 설치 중..."
    cd frontend
    flutter pub get
    cd ..
    echo "✅ Frontend 의존성 설치 완료"
fi

echo "
✅ 개발 환경 설정 완료!

다음 명령어로 서비스를 실행하세요:

Backend:
  cd backend && ./gradlew bootRun

Frontend:
  cd frontend && flutter run

Docker 서비스 확인:
  docker compose -f docker-compose/docker-compose.yml ps

Docker 서비스 종료:
  docker compose -f docker-compose/docker-compose.yml down
"
