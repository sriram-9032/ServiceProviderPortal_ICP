package com.dtt.organization.util;

import com.dtt.organization.dto.ApiResponses;


import java.text.SimpleDateFormat;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;

import java.util.Date;


public class AppUtil {

    private AppUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String getDate(){
        SimpleDateFormat smpdate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return smpdate.format(date);
    }


    public static String formatDate(String date){

        String datePart = date.substring(0, 10);
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        LocalDate dateNew = LocalDate.parse(datePart, inputFormatter);
        return dateNew.format(outputFormatter);

    }

    public static ApiResponses createApiResponses(boolean success, String msg, Object object) {
        ApiResponses apiResponses = new ApiResponses();
        apiResponses.setMessage(msg);
        apiResponses.setResult(object);
        apiResponses.setSuccess(success);
        return apiResponses;
    }

}