package com.mojang.authlib.yggdrasil;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super();
    }
    
    public ProfileNotFoundException(String message) {
        super(message);
    }
    
    public ProfileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ProfileNotFoundException(Throwable cause) {
        super(cause);
    }

}
