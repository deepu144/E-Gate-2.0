package com.kce.egate.util;

import com.kce.egate.constant.Constant;
import com.kce.egate.entity.BatchInformation;
import com.kce.egate.util.exceptions.DuplicateInformationFoundException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {
    public static Set<BatchInformation> uploadBatchInformation(MultipartFile multipartFile) throws IOException, DuplicateInformationFoundException {
        Set<BatchInformation> batchInformationList = new HashSet<>();
        try(InputStream inputStream = multipartFile.getInputStream()){
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            for(Row row : sheet){
                BatchInformation batchInformation = new BatchInformation();
                batchInformation.setRollNumber(row.getCell(0).getStringCellValue());
                batchInformation.setName(row.getCell(1).getStringCellValue());
                batchInformation.setDept(row.getCell(2).getStringCellValue());
                batchInformation.setBatch(row.getCell(3).getStringCellValue());
                if(!batchInformationList.add(batchInformation)){
                    throw new DuplicateInformationFoundException(Constant.DUPLICATE_INFORMATION_FOUND);
                }
            }
        }
        return batchInformationList;
    }
}
