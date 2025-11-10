package com.mycompany.myapp.service.dto;

import java.io.Serializable;
import java.util.Objects;

public class CustomerInfoDTO implements Serializable {

    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String paymentMethod;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerInfoDTO that = (CustomerInfoDTO) o;
        return (
            Objects.equals(fullName, that.fullName) &&
            Objects.equals(phone, that.phone) &&
            Objects.equals(email, that.email) &&
            Objects.equals(address, that.address) &&
            Objects.equals(paymentMethod, that.paymentMethod)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName, phone, email, address, paymentMethod);
    }

    @Override
    public String toString() {
        return (
            "CustomerInfoDTO{" +
            "fullName='" +
            fullName +
            "\'" +
            ", phone='" +
            phone +
            "\'" +
            ", email='" +
            email +
            "\'" +
            ", address='" +
            address +
            "\'" +
            ", paymentMethod='" +
            paymentMethod +
            "\'}"
        );
    }
}
