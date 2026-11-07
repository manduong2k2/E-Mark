package com.Marketplace_Management.Vendor.DTOs.Response;

import java.util.UUID;

import com.Marketplace_Management.Vendor.Entities.Vendor;
import com.Marketplace_Management.Vendor.Constants.VendorStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.Marketplace_Management.Shared.Utils.Helpers.UrlHelper;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorResponse {
    private UUID id;
    private UUID userId;
    private String name;
    private VendorStatus status;
    private String description;
    private String logo;
    private String banner;
    private String taxCode;
    private String email;
    private UUID addressId;
    private String phone;

    public VendorResponse(Vendor vendor) {
        if (vendor == null) {
            return;
        }
        this.id             = vendor.getId();
        this.userId         = vendor.getUserId();
        this.name           = vendor.getName();
        this.status         = vendor.getStatus();
        this.description    = vendor.getDescription();
        this.logo           = vendor.getLogo();
        this.banner         = vendor.getBanner();
        this.taxCode        = vendor.getTaxCode();
        this.email          = vendor.getEmail();
        this.addressId      = vendor.getAddressId();
        this.phone          = vendor.getPhone();
    }

    public VendorResponse withUrl(String baseUrl) {
        this.logo = UrlHelper.toPublicUrl(baseUrl, this.logo);
        this.banner = UrlHelper.toPublicUrl(baseUrl, this.banner);
        return this;
    }
}
