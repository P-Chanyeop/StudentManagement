#!/bin/bash

# 관리자 로그인
echo "관리자 로그인..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "로그인 실패"
  exit 1
fi

echo "로그인 성공"
echo ""

# 엑셀 파일 업로드
echo "학생 반 정보 임포트..."
IMPORT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/students/import-courses \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@src/main/resources/static/images/리틀베어_리딩클럽_학생목록 (16).xlsx")

echo "결과:"
echo $IMPORT_RESPONSE | python3 -m json.tool

