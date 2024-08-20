package com.kce.egate.util;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.Batch;
import com.kce.egate.entity.BatchInformation;
import com.kce.egate.repository.BatchRepository;
import com.kce.egate.response.EntryObject;
import com.kce.egate.service.serviceImpl.AdminServiceImpl;
import com.kce.egate.util.exceptions.InvalidFilterException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
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
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        String fileName = BACKUP_DIR + "EntryDataBackup_" + timeStamp + ".xlsx";
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Entry BackUp Data");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("S.No");
            headerRow.createCell(1).setCellValue("Roll Number");
            headerRow.createCell(2).setCellValue("Name");
            headerRow.createCell(3).setCellValue("Department");
            headerRow.createCell(4).setCellValue("In Date");
            headerRow.createCell(5).setCellValue("In Time");
            headerRow.createCell(6).setCellValue("Out Date");
            headerRow.createCell(7).setCellValue("Out Time");
            int rowNum = 0;
            for(String batch : batches){
                List<EntryObject> entryObjects = adminService.getAllEntryObject(null, LocalDate.now(),LocalDate.now().minusDays(7),null,null,null);
                for(EntryObject entryObject : entryObjects){
                    Query query = new Query();
                    query.addCriteria(Criteria.where("rollNumber").is(entryObject.getRollNumber()));
                    BatchInformation batchInformation = mongoTemplate.findOne(query, BatchInformation.class,batch);
                    if(batchInformation!=null){
                        Row row = sheet.createRow(++rowNum);
                        row.createCell(0).setCellValue(rowNum);
                        row.createCell(1).setCellValue(entryObject.getRollNumber());
                        row.createCell(2).setCellValue(batchInformation.getName());
                        row.createCell(3).setCellValue(batchInformation.getDept());
                        row.createCell(4).setCellValue(entryObject.getInDate());
                        row.createCell(5).setCellValue(LocalDateTime.from(entryObject.getInTime()));
                        row.createCell(6).setCellValue(entryObject.getOutDate());
                        row.createCell(7).setCellValue(LocalDateTime.from(entryObject.getOutTime()));
                    }
                }
            }
            File directory = new File(BACKUP_DIR);
            if (!directory.exists()) {
                boolean isDirCreated = directory.mkdir();
                if(!isDirCreated){
                    throw new Exception(Constant.DIRECTORY_NOT_CREATED);
                }
            }
            FileOutputStream fileOut = new FileOutputStream(fileName);
            workbook.write(fileOut);
            File backupFile = new File(fileName);
            if (backupFile.exists() && backupFile.length() > 0) {
                System.out.println("Backup created successfully: " + fileName);
            } else {
                System.err.println("Backup validation failed for: " + fileName);
            }
        }catch (Exception ignored){
            System.out.println("NOOOO...........");
        };
    }
}
