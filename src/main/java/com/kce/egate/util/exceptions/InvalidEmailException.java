package com.kce.egate.util.exceptions;

public class InvalidEmailException extends Exception{
    public InvalidEmailException(String message){
        super(message);
    }
}