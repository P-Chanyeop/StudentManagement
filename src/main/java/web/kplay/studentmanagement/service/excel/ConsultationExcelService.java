package web.kplay.studentmanagement.service.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import web.kplay.studentmanagement.domain.consultation.Consultation;
import web.kplay.studentmanagement.repository.ConsultationRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConsultationExcelService {

    private final ConsultationRepository consultationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 특정 학생의 상담 이력을 Excel 파일로 내보내기
     */
    public byte[] exportConsultationsByStudent(Long studentId) throws IOException {
        try {
            log.info("=== Excel Export Debug ===");
            log.info("Student ID: {}", studentId);
            
            // 1단계: 데이터 조회 테스트
            List<Consultation> consultations = consultationRepository.findByStudentIdOrderByDateDesc(studentId);
            log.info("Found consultations: {}", consultations.size());
            
            if (consultations.isEmpty()) {
                log.warn("No consultations found for student ID: {}", studentId);
                // 빈 데이터라도 Excel 생성
                return generateEmptyStudentExcel(studentId);
            }
            
            // 2단계: 첫 번째 상담 데이터 확인
            Consultation firstConsultation = consultations.get(0);
            log.info("First consultation - ID: {}, Date: {}, Title: {}", 
                firstConsultation.getId(), 
                firstConsultation.getConsultationDate(),
                firstConsultation.getTitle());
            
            // 3단계: 학생 정보 확인
            String studentName = firstConsultation.getStudent().getStudentName();
            log.info("Student name: {}", studentName);
            
            // 4단계: Excel 생성
            return generateStudentExcel(consultations, studentName);
            
        } catch (Exception e) {
            log.error("=== Excel Export Error ===");
            log.error("Student ID: {}", studentId);
            log.error("Error type: {}", e.getClass().getSimpleName());
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace: ", e);
            throw new IOException("Excel 파일 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 빈 학생 Excel 생성 (데이터가 없을 때)
     */
    private byte[] generateEmptyStudentExcel(Long studentId) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("상담이력");
            
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("학생 ID " + studentId + " - 상담 이력이 없습니다");
            
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 전체 상담 이력을 Excel 파일로 내보내기
     */
    public byte[] exportAllConsultations() throws IOException {
        try {
            log.info("Exporting all consultations");
            List<Consultation> consultations = consultationRepository.findAllByOrderByConsultationDateDesc();
            log.info("Found {} total consultations", consultations.size());
            
            return generateAllConsultationsExcel(consultations);
            
        } catch (Exception e) {
            log.error("Error exporting all consultations", e);
            throw new IOException("Excel 파일 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 기간별 상담 이력을 Excel 파일로 내보내기
     */
    public byte[] exportConsultationsByDateRange(LocalDate startDate, LocalDate endDate) throws IOException {
        try {
            log.info("Exporting consultations for date range: {} to {}", startDate, endDate);
            List<Consultation> consultations = consultationRepository.findByDateRange(startDate, endDate);
            log.info("Found {} consultations for date range", consultations.size());
            
            return generateDateRangeExcel(consultations, startDate, endDate);
            
        } catch (Exception e) {
            log.error("Error exporting consultations for date range: {} to {}", startDate, endDate, e);
            throw new IOException("Excel 파일 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 학생별 상담 이력 Excel 생성
     */
    private byte[] generateStudentExcel(List<Consultation> consultations, String studentName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(studentName + " 상담이력");
            
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;
            
            // 제목
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(studentName + " 학생 상담 이력");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            
            rowNum++;
            
            // 요약
            Row summaryRow = sheet.createRow(rowNum++);
            Cell summaryCell = summaryRow.createCell(0);
            summaryCell.setCellValue("총 상담 횟수: " + consultations.size() + "회");
            summaryCell.setCellStyle(dataStyle);
            
            rowNum++;

            // 헤더
            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"번호", "상담일자", "상담 제목", "상담 내용", "상담자"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터
            int dataRowNum = 1;
            for (Consultation consultation : consultations) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(dataRowNum++);
                row.createCell(1).setCellValue(consultation.getConsultationDate().format(DATE_FORMATTER));
                row.createCell(2).setCellValue(consultation.getTitle() != null ? consultation.getTitle() : "");
                row.createCell(3).setCellValue(consultation.getContent() != null ? consultation.getContent() : "");
                row.createCell(4).setCellValue(consultation.getConsultant() != null ? consultation.getConsultant().getName() : "");
                
                for (int i = 0; i < 5; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 전체 상담 이력 Excel 생성
     */
    private byte[] generateAllConsultationsExcel(List<Consultation> consultations) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("전체 상담 이력");
            
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;
            
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("전체 학생 상담 이력 현황");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            
            rowNum++;
            
            Row summaryRow = sheet.createRow(rowNum++);
            summaryRow.createCell(0).setCellValue("총 상담 건수: " + consultations.size() + "건");
            summaryRow.createCell(3).setCellValue("출력일: " + LocalDate.now().format(DATE_FORMATTER));
            
            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"번호", "학생명", "상담일자", "상담 제목", "상담 내용", "상담자"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int dataRowNum = 1;
            for (Consultation consultation : consultations) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(dataRowNum++);
                row.createCell(1).setCellValue(consultation.getStudent().getStudentName());
                row.createCell(2).setCellValue(consultation.getConsultationDate().format(DATE_FORMATTER));
                row.createCell(3).setCellValue(consultation.getTitle() != null ? consultation.getTitle() : "");
                row.createCell(4).setCellValue(consultation.getContent() != null ? consultation.getContent() : "");
                row.createCell(5).setCellValue(consultation.getConsultant() != null ? consultation.getConsultant().getName() : "");
                
                for (int i = 0; i < 6; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * 기간별 상담 이력 Excel 생성
     */
    private byte[] generateDateRangeExcel(List<Consultation> consultations, LocalDate startDate, LocalDate endDate) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("기간별 상담 이력");
            
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int rowNum = 0;
            
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("기간별 상담 이력 (" + startDate.format(DATE_FORMATTER) + " ~ " + endDate.format(DATE_FORMATTER) + ")");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            
            rowNum++;
            
            Row summaryRow = sheet.createRow(rowNum++);
            summaryRow.createCell(0).setCellValue("조회 기간: " + startDate.format(DATE_FORMATTER) + " ~ " + endDate.format(DATE_FORMATTER));
            Row summaryRow2 = sheet.createRow(rowNum++);
            summaryRow2.createCell(0).setCellValue("총 상담 건수: " + consultations.size() + "건");
            summaryRow2.createCell(3).setCellValue("출력일: " + LocalDate.now().format(DATE_FORMATTER));
            
            rowNum++;

            Row headerRow = sheet.createRow(rowNum++);
            String[] headers = {"번호", "학생명", "상담일자", "상담 제목", "상담 내용", "상담자"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int dataRowNum = 1;
            for (Consultation consultation : consultations) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(dataRowNum++);
                row.createCell(1).setCellValue(consultation.getStudent().getStudentName());
                row.createCell(2).setCellValue(consultation.getConsultationDate().format(DATE_FORMATTER));
                row.createCell(3).setCellValue(consultation.getTitle() != null ? consultation.getTitle() : "");
                row.createCell(4).setCellValue(consultation.getContent() != null ? consultation.getContent() : "");
                row.createCell(5).setCellValue(consultation.getConsultant() != null ? consultation.getConsultant().getName() : "");
                
                for (int i = 0; i < 6; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        
        return style;
    }
}
