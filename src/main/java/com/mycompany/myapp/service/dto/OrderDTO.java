package com.mycompany.myapp.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class OrderDTO implements Serializable {

    private CustomerInfoDTO customerInfo;
    private List<OrderItemDTO> items;
    private Double totalAmount; // Hoàn tác về Double
    private Instant orderDate;
    private String status;
    private String notes;

    public CustomerInfoDTO getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(CustomerInfoDTO customerInfo) {
        this.customerInfo = customerInfo;
    }

    public List<OrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDTO> items) {
        this.items = items;
    }

    public Double getTotalAmount() { // Hoàn tác về Double
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) { // Hoàn tác về Double
        this.totalAmount = totalAmount;
    }

    public Instant getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(Instant orderDate) {
        this.orderDate = orderDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderDTO orderDTO = (OrderDTO) o;
        return (
            Objects.equals(customerInfo, orderDTO.customerInfo) &&
            Objects.equals(items, orderDTO.items) &&
            Objects.equals(totalAmount, orderDTO.totalAmount) &&
            Objects.equals(orderDate, orderDTO.orderDate) &&
            Objects.equals(status, orderDTO.status) &&
            Objects.equals(notes, orderDTO.notes)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerInfo, items, totalAmount, orderDate, status, notes);
    }

    @Override
    public String toString() {
        return (
            "OrderDTO{" +
            "customerInfo=" +
            customerInfo +
            ", items=" +
            items +
            ", totalAmount=" +
            totalAmount +
            ", orderDate=" +
            orderDate +
            ", status='" +
            status +
            "', notes='" +
            notes +
            "\'}"
        );
    }
}
