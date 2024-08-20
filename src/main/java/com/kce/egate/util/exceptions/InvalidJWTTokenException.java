package com.kce.egate.util.exceptions;

public class InvalidJWTTokenException extends Exception{
    public InvalidJWTTokenException(String message){
        super(message);
    }
}
