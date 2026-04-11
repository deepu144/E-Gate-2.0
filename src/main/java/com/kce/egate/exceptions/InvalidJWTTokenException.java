package com.kce.egate.exceptions;

public class InvalidJWTTokenException extends Exception{
    public InvalidJWTTokenException(String message){
        super(message);
    }
}
