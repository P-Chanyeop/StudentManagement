#!/bin/bash

# 관리자 로그인 (토큰 받기)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.accessToken')

echo "Token: $TOKEN"

# 기존 A반 조회 및 삭제
echo "Deleting existing courses..."
COURSES=$(curl -s -X GET http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN")

echo "$COURSES" | jq -r '.[] | select(.courseName == "A반") | .id' | while read id; do
  echo "Deleting course ID: $id"
  curl -X DELETE "http://localhost:8080/api/courses/$id" \
    -H "Authorization: Bearer $TOKEN"
done

# 새로운 4개 반 생성
echo "Creating Able class..."
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "Able",
    "description": "Able 반 - 60분 수업",
    "maxStudents": 6,
    "durationMinutes": 60,
    "level": "Able",
    "color": "#4CAF50",
    "isActive": true
  }'

echo "Creating Basic class..."
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "Basic",
    "description": "Basic 반 - 90분 수업",
    "maxStudents": 6,
    "durationMinutes": 90,
    "level": "Basic",
    "color": "#2196F3",
    "isActive": true
  }'

echo "Creating Core class..."
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "Core",
    "description": "Core 반 - 120분 수업",
    "maxStudents": 6,
    "durationMinutes": 120,
    "level": "Core",
    "color": "#FF9800",
    "isActive": true
  }'

echo "Creating Development class..."
curl -X POST http://localhost:8080/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "Development",
    "description": "Development 반 - 150분 수업",
    "maxStudents": 6,
    "durationMinutes": 150,
    "level": "Development",
    "color": "#9C27B0",
    "isActive": true
  }'

echo "Done!"
