package com.kce.egate.util;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.BatchInformation;
import com.kce.egate.exceptions.DuplicateInformationFoundException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    public static Set<BatchInformation> uploadBatchInformation(MultipartFile multipartFile) throws IOException {
        log.debug("[SERVICE] Starting to process batch information upload");
        Set<String> rollNumbers = new HashSet<>();
        Set<BatchInformation> batchInformationList = new HashSet<>();
        log.debug("[SERVICE] Parsing csv file to Objects");
        try(InputStream inputStream = multipartFile.getInputStream()){
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            for(Row row : sheet){
                String rollNumber = row.getCell(0).getStringCellValue();
                if(!rollNumbers.add(rollNumber)){
                    log.warn("Duplicate roll number {} found, Skipping", rollNumber);
                    continue;
                }
                BatchInformation batchInformation = new BatchInformation();
                batchInformation.setRollNumber(rollNumber);
                batchInformation.setName(row.getCell(1).getStringCellValue());
                batchInformation.setDept(row.getCell(2).getStringCellValue());
                batchInformation.setBatch(row.getCell(3).getStringCellValue());
            }
        }
        return batchInformationList;
    }
}
