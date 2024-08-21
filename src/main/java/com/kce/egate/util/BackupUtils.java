package com.kce.egate.util;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.Batch;
import com.kce.egate.entity.BatchInformation;
import com.kce.egate.repository.BatchRepository;
import com.kce.egate.response.EntryObject;
import com.kce.egate.service.serviceImpl.AdminServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BackupUtils {
    private final AdminServiceImpl adminService;
    private final MongoTemplate mongoTemplate;
    private final BatchRepository batchRepository;
    private static final String BACKUP_DIR = "C:/MyAppBackups/";

    public void backupDB() {
        List<String> batches = batchRepository.findAll()
                .parallelStream()
                .map(Batch::getBatchName)
                .toList();
        LocalDate today = LocalDate.now();
        String fileName = BACKUP_DIR + "Backup_" + today.minusDays(7) + "_" + today + ".xlsx";
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Entry BackUp Data");
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontName("Times New Roman");
            boldStyle.setFont(boldFont);
            CellStyle normalStyle = workbook.createCellStyle();
            Font normalFont = workbook.createFont();
            normalFont.setFontName("Times New Roman");
            normalStyle.setFont(normalFont);
            Row headerRow = sheet.createRow(0);
            String[] headers = {"S.No", "Roll Number", "Name", "Department", "In Date", "In Time", "Out Date", "Out Time", "Batch"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }
            int rowNum = 0;
            for (String batch : batches) {
                List<EntryObject> entryObjects = adminService.getAllEntryObject(null, today.minusDays(7), today, null, null, null);
                for (EntryObject entryObject : entryObjects) {
                    Query query = new Query();
                    query.addCriteria(Criteria.where("rollNumber").is(entryObject.getRollNumber()));
                    BatchInformation batchInformation = mongoTemplate.findOne(query, BatchInformation.class, batch + "_Information");
                    if (batchInformation != null) {
                        Row row = sheet.createRow(++rowNum);
                        Cell cell0 = row.createCell(0);
                        cell0.setCellValue(rowNum);
                        cell0.setCellStyle(normalStyle);

                        Cell cell1 = row.createCell(1);
                        cell1.setCellValue(entryObject.getRollNumber());
                        cell1.setCellStyle(normalStyle);

                        Cell cell2 = row.createCell(2);
                        cell2.setCellValue(batchInformation.getName());
                        cell2.setCellStyle(normalStyle);

                        Cell cell3 = row.createCell(3);
                        cell3.setCellValue(batchInformation.getDept());
                        cell3.setCellStyle(normalStyle);

                        Cell cell4 = row.createCell(4);
                        cell4.setCellValue(entryObject.getInDate()+"");
                        cell4.setCellStyle(normalStyle);

                        Cell cell5 = row.createCell(5);
                        cell5.setCellValue(entryObject.getInTime()+"");
                        cell5.setCellStyle(normalStyle);

                        Cell cell6 = row.createCell(6);
                        cell6.setCellValue(entryObject.getOutDate()+"");
                        cell6.setCellStyle(normalStyle);

                        Cell cell7 = row.createCell(7);
                        cell7.setCellValue(entryObject.getOutTime()+"");
                        cell7.setCellStyle(normalStyle);

                        Cell cell8 = row.createCell(8);
                        cell8.setCellValue(batchInformation.getBatch());
                        cell8.setCellStyle(normalStyle);
                    }
                }
            }
            File directory = new File(BACKUP_DIR);
            if (!directory.exists()) {
                boolean isDirCreated = directory.mkdir();
                if (!isDirCreated) {
                    throw new IOException(Constant.DIRECTORY_NOT_CREATED);
                }
            }
            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            }
        } catch (Exception ignored) {}
    }
}
