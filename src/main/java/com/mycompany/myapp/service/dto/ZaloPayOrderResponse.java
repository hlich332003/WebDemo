package com.mycompany.myapp.service.dto;

import java.io.Serializable;

/**
 * DTO for ZaloPay payment response
 */
public class ZaloPayOrderResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer returnCode;
    private String returnMessage;
    private String orderUrl;
    private String zpTransToken;

    public ZaloPayOrderResponse() {}

    // Getters and Setters
    public Integer getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(Integer returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public String getOrderUrl() {
        return orderUrl;
    }

    public void setOrderUrl(String orderUrl) {
        this.orderUrl = orderUrl;
    }

    public String getZpTransToken() {
        return zpTransToken;
    }

    public void setZpTransToken(String zpTransToken) {
        this.zpTransToken = zpTransToken;
    }

    @Override
    public String toString() {
        return (
            "ZaloPayOrderResponse{" +
            "returnCode=" +
            returnCode +
            ", returnMessage='" +
            returnMessage +
            '\'' +
            ", orderUrl='" +
            orderUrl +
            '\'' +
            ", zpTransToken='" +
            zpTransToken +
            '\'' +
            '}'
        );
    }
}
