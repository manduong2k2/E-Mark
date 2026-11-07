package com.Marketplace_Management.Delivery.Repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.Marketplace_Management.Delivery.Contracts.IAddressRepository;
import com.Marketplace_Management.Delivery.Entities.Address;
import com.Marketplace_Management.Delivery.Entities.Province;
import com.Marketplace_Management.Delivery.Entities.Ward;

@Repository
public class AddressRepository implements IAddressRepository {
    private final JpaAddressRepository addressRepository;
    private final JpaProvinceRepository provinceRepository;
    private final JpaWardRepository wardRepository;

    public AddressRepository(JpaAddressRepository addressRepository, JpaProvinceRepository provinceRepository,
            JpaWardRepository wardRepository) {
        this.addressRepository = addressRepository;
        this.provinceRepository = provinceRepository;
        this.wardRepository = wardRepository;
    }

    @Override
    public List<Province> findProvinces() {
        return provinceRepository.findAll();
    }

    @Override
    public List<Ward> findWardsByProvince(String provinceId) {
        return wardRepository.findByProvinceId(provinceId);
    }

    @Override
    public Optional<Ward> findWardById(String wardId) {
        return wardRepository.findById(wardId);
    }

    @Override
    public List<Address> findByUser(UUID userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    @Override
    public Optional<Address> findByIdAndUser(Long addressId, UUID userId) {
        return addressRepository.findByIdAndUserId(addressId, userId);
    }

    @Override
    public Optional<Address> findDefault(UUID userId) {
        return addressRepository.findFirstByUserIdAndIsDefaultTrue(userId);
    }

    @Override
    public Optional<Address> findLatest(UUID userId) {
        return addressRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Address save(Address address) {
        return addressRepository.saveAndFlush(address);
    }

    @Override
    public void delete(Long addressId) {
        addressRepository.deleteById(addressId);
    }

    @Override
    public void clearDefault(UUID userId) {
        addressRepository.clearDefault(userId);
    }
}
