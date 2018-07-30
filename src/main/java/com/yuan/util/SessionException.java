package com.yuan.util;

public class SessionException extends Exception {
    public SessionException(String msg) {
        super(msg);
    }

    public SessionException(String msg, Exception e) {
        super(msg, e);
    }
}
