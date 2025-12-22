package com.mycompany.myapp.web.rest.errors;

public class PhoneAlreadyUsedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PhoneAlreadyUsedException() {
        super("Số điện thoại đã được sử dụng!");
    }
}
