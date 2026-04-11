package com.kce.egate.exceptions;

public class PasswordNotMatchException extends Exception{
    public PasswordNotMatchException(String message){
        super(message);
    }
}
