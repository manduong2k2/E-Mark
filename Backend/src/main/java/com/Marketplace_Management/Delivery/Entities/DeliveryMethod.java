package com.Marketplace_Management.Delivery.Entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import com.Marketplace_Management.Shared.Entities.NumericEntity;

import lombok.EqualsAndHashCode;

@Entity
@Table(name = "delivery_methods")
@Data
@EqualsAndHashCode(callSuper = false)
public class DeliveryMethod extends NumericEntity{
    private String name;
    private String description;
    private double unitFee;
}
