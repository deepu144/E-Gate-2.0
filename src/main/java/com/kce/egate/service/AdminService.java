package com.kce.egate.service;

import com.kce.egate.response.CommonResponse;
import com.kce.egate.util.exceptions.InvalidFilterException;

import java.time.LocalDate;

public interface AdminService {
    CommonResponse getAllEntry(
            String rollNumber,
            LocalDate fromDate,
            LocalDate toDate,
            String batch,
            int page,
            int size,
            String order,
            String orderBy
    ) throws InvalidFilterException;

    CommonResponse addBatch(String batch);

    CommonResponse getAllBatch();

    CommonResponse deleteBatch(String batch) throws ClassNotFoundException;
}
