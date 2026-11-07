package com.Marketplace_Management.Delivery.Contracts;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.Marketplace_Management.Delivery.Entities.Address;
import com.Marketplace_Management.Delivery.Entities.Province;
import com.Marketplace_Management.Delivery.Entities.Ward;

public interface IAddressRepository {
    List<Province> findProvinces();
    List<Ward> findWardsByProvince(String provinceId);
    Optional<Ward> findWardById(String wardId);

    List<Address> findByUser(UUID userId);
    /** Only returns the address when it belongs to the user. */
    Optional<Address> findByIdAndUser(Long addressId, UUID userId);
    Optional<Address> findDefault(UUID userId);
    Optional<Address> findLatest(UUID userId);

    Address save(Address address);
    void delete(Long addressId);
    void clearDefault(UUID userId);
}
