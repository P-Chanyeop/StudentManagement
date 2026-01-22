-- 기존 A반 삭제
DELETE FROM courses WHERE course_name = 'A반';

-- 새로운 4개 반 생성
INSERT INTO courses (course_name, description, max_students, duration_minutes, level, color, is_active, created_at, updated_at)
VALUES 
('Able', 'Able 반 - 60분 수업', 6, 60, 'Able', '#4CAF50', true, NOW(), NOW()),
('Basic', 'Basic 반 - 90분 수업', 6, 90, 'Basic', '#2196F3', true, NOW(), NOW()),
('Core', 'Core 반 - 120분 수업', 6, 120, 'Core', '#FF9800', true, NOW(), NOW()),
('Development', 'Development 반 - 150분 수업', 6, 150, 'Development', '#9C27B0', true, NOW(), NOW());
