package com.mycompany.myapp.service.dto;

import java.io.Serializable;

public class DashboardStatsDTO implements Serializable {

    private Double totalRevenue;
    private Long totalOrders;
    private Long totalCustomers;
    private Long totalProducts;

    public DashboardStatsDTO() {}

    public DashboardStatsDTO(Double totalRevenue, Long totalOrders, Long totalCustomers, Long totalProducts) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.totalCustomers = totalCustomers;
        this.totalProducts = totalProducts;
    }

    public Double getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(Long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public Long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(Long totalProducts) {
        this.totalProducts = totalProducts;
    }
}
