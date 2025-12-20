package web.kplay.studentmanagement.service.excel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
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
        List<Consultation> consultations = consultationRepository.findByStudentId(studentId);
        return generateExcel(consultations, "학생별 상담 이력");
    }

    /**
     * 전체 상담 이력을 Excel 파일로 내보내기
     */
    public byte[] exportAllConsultations() throws IOException {
        List<Consultation> consultations = consultationRepository.findAll();
        return generateExcel(consultations, "전체 상담 이력");
    }

    /**
     * 기간별 상담 이력을 Excel 파일로 내보내기
     */
    public byte[] exportConsultationsByDateRange(LocalDate startDate, LocalDate endDate) throws IOException {
        List<Consultation> consultations = consultationRepository
                .findByDateRange(startDate, endDate);
        return generateExcel(consultations, "기간별 상담 이력");
    }

    /**
     * Excel 파일 생성
     */
    private byte[] generateExcel(List<Consultation> consultations, String sheetName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            // 스타일 생성
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {"번호", "학생명", "상담일시", "상담 내용", "상담자", "비고"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 입력
            int rowNum = 1;
            for (Consultation consultation : consultations) {
                Row row = sheet.createRow(rowNum++);

                // 번호
                Cell cell0 = row.createCell(0);
                cell0.setCellValue(rowNum - 1);
                cell0.setCellStyle(dataStyle);

                // 학생명
                Cell cell1 = row.createCell(1);
                cell1.setCellValue(consultation.getStudent().getStudentName());
                cell1.setCellStyle(dataStyle);

                // 상담일시
                Cell cell2 = row.createCell(2);
                String formattedDate = consultation.getConsultationDate().format(DATE_FORMATTER);
                cell2.setCellValue(formattedDate);
                cell2.setCellStyle(dateStyle);

                // 상담 내용
                Cell cell3 = row.createCell(3);
                cell3.setCellValue(consultation.getContent() != null ? consultation.getContent() : "");
                cell3.setCellStyle(dataStyle);

                // 상담자
                Cell cell4 = row.createCell(4);
                cell4.setCellValue(consultation.getConsultant() != null ? consultation.getConsultant().getName() : "");
                cell4.setCellStyle(dataStyle);

                // 비고 (후속 조치 사항)
                Cell cell5 = row.createCell(5);
                cell5.setCellValue(consultation.getActionItems() != null ? consultation.getActionItems() : "");
                cell5.setCellStyle(dataStyle);
            }

            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 최소/최대 너비 설정
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth < 2000) {
                    sheet.setColumnWidth(i, 2000);
                } else if (currentWidth > 15000) {
                    sheet.setColumnWidth(i, 15000);
                }
            }

            workbook.write(out);
            log.info("Excel 파일 생성 완료: 시트={}, 건수={}", sheetName, consultations.size());
            return out.toByteArray();
        }
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 배경색 설정
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 테두리 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 정렬 설정
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 폰트 설정
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }

    /**
     * 데이터 스타일 생성
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 테두리 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 정렬 설정
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 텍스트 줄바꿈 허용
        style.setWrapText(true);

        return style;
    }

    /**
     * 날짜 스타일 생성
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
}
