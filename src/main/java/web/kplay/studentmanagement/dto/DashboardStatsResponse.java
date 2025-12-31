package web.kplay.studentmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private int totalStudents;
    private int todaySchedules;
    private int todayAttendance;
    private double attendanceRate;
    private int expiringEnrollments;

    @Override
    public String toString() {
        return String.format("DashboardStats{totalStudents=%d, todaySchedules=%d, todayAttendance=%d, attendanceRate=%.1f%%, expiringEnrollments=%d}", 
                totalStudents, todaySchedules, todayAttendance, attendanceRate, expiringEnrollments);
    }
}
