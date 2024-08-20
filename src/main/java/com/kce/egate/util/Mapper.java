package com.kce.egate.util;

import com.kce.egate.entity.Batch;
import com.kce.egate.entity.DailyUtils;
import com.kce.egate.entity.Entry;
import com.kce.egate.response.BatchObject;
import com.kce.egate.response.DailyUtilsObject;
import com.kce.egate.response.EntryObject;

public final class Mapper {

    public static BatchObject convertToBatchObject(Batch batch) {
        BatchObject batchObject = new BatchObject();
        batchObject.setBatchName(batch.getBatchName());
        batchObject.setUniqueId(batch.getUniqueId());
        return batchObject;
    }
    public static EntryObject convertToEntryObject(Entry entry){
        EntryObject entryObject = new EntryObject();
        entryObject.setRollNumber(entry.getRollNumber());
        entryObject.setStatus(entry.getStatus());
        entryObject.setOutTime(entry.getOutTime());
        entryObject.setOutDate(entry.getOutDate());
        entryObject.setInTime(entry.getInTime());
        entryObject.setInDate(entry.getInDate());
        return entryObject;
    }

    public static DailyUtilsObject convertToDailyUtilsObject(DailyUtils dailyUtils) {
        return DailyUtilsObject.builder()
                .studentInCount(dailyUtils.getStudentInCount())
                .studentOutCount(dailyUtils.getStudentOutCount())
                .staffInCount(dailyUtils.getStaffInCount())
                .staffOutCount(dailyUtils.getStaffOutCount())
                .build();
    }
}
