-- 홍길동(studentId: 1) 1월 2일~12일 출석 데이터 생성
-- 기존 데이터 삭제
DELETE FROM attendances WHERE student_id = 1;

-- 1월 2일 - 출석 (PRESENT)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 2, 'PRESENT', '2026-01-02 14:00:00', NULL, '16:00:00', '16:00:00', '', NULL, false, '', '', false, false, false, false);

-- 1월 3일 - 지각 (LATE)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 3, 'LATE', '2026-01-03 14:15:00', NULL, '16:00:00', '16:00:00', '', NULL, false, '', '', false, false, false, false);

-- 1월 6일 - 결석 (ABSENT)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 4, 'ABSENT', NULL, NULL, NULL, NULL, '', '무단결석', false, '', '', false, false, false, false);

-- 1월 7일 - 출석 (PRESENT)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 5, 'PRESENT', '2026-01-07 14:00:00', NULL, '16:00:00', '16:00:00', '', NULL, false, '', '', false, false, false, false);

-- 1월 8일 - 지각 (LATE)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 6, 'LATE', '2026-01-08 14:20:00', NULL, '16:00:00', '16:00:00', '', NULL, false, '', '', false, false, false, false);

-- 1월 9일 - 출석 (PRESENT)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 7, 'PRESENT', '2026-01-09 14:00:00', NULL, '16:00:00', '16:00:00', '', NULL, false, '', '', false, false, false, false);

-- 1월 10일 - 결석 (ABSENT)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 8, 'ABSENT', NULL, NULL, NULL, NULL, '', '병결', false, '', '', false, false, false, false);

-- 1월 12일 - 지각 (LATE)
INSERT INTO attendances (student_id, schedule_id, status, check_in_time, check_out_time, expected_leave_time, original_expected_leave_time, memo, reason, class_completed, dc_check, wr_check, vocabulary_class, grammar_class, phonics_class, speaking_class) 
VALUES (1, 10, 'LATE', '2026-01-12 14:10:00', NULL, '16:00:00', '16:00:00', '', NULL, false, '', '', false, false, false, false);
