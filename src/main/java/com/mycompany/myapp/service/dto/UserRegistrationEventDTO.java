package com.mycompany.myapp.service.dto;

import java.io.Serializable;

public class UserRegistrationEventDTO implements Serializable {

    private String email;
    private String firstName;
    private String lastName;
    private String activationKey;

    public UserRegistrationEventDTO() {}

    public UserRegistrationEventDTO(String email, String firstName, String lastName, String activationKey) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.activationKey = activationKey;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }
}
