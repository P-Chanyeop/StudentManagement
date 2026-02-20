-- 수강권 조정 이력 테이블
CREATE TABLE enrollment_adjustments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id BIGINT NOT NULL,
    adjustment_type ENUM('DEDUCT', 'ADD', 'RESTORE') NOT NULL,
    count_change INT NOT NULL,
    reason VARCHAR(500),
    admin_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(id),
    FOREIGN KEY (admin_id) REFERENCES users(id)
);

-- 예약 상태에 AUTO_DEDUCTED 추가 (자동 차감된 예약)
ALTER TABLE reservations 
MODIFY COLUMN status ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW', 'AUTO_DEDUCTED') NOT NULL;
