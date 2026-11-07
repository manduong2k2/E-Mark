package com.Marketplace_Management.Delivery.Services;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.Marketplace_Management.Delivery.Contracts.IAddressRepository;
import com.Marketplace_Management.Delivery.Contracts.IAddressService;
import com.Marketplace_Management.Delivery.DTOs.Commands.Address.SaveAddressCommand;
import com.Marketplace_Management.Delivery.DTOs.Response.Address.AddressResponse;
import com.Marketplace_Management.Delivery.DTOs.Response.Address.RegionResponse;
import com.Marketplace_Management.Delivery.Entities.Address;
import com.Marketplace_Management.Delivery.Entities.Province;
import com.Marketplace_Management.Delivery.Entities.Ward;
import com.Marketplace_Management.Shared.Errors.Exceptions.BadRequestException;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;

/**
 * A user's address book. Invariant: as soon as a user has an address, exactly one of them is the default
 * (the first address becomes the default, the default cannot be unset, only replaced, and deleting it
 * promotes the most recent remaining address). Every lookup is scoped to the current user.
 */
@Service
public class AddressService implements IAddressService {
    private static final String ADDRESS_NOT_FOUND = "Address not found";
    private static final String DEFAULT_ADDRESS_NOT_FOUND = "Default address not found";
    private static final String WARD_NOT_FOUND = "The selected ward does not exist";

    // Vietnamese alphabetical order (accents are secondary differences)
    private static final Comparator<RegionResponse> BY_NAME =
            Comparator.comparing(RegionResponse::getName, Collator.getInstance(Locale.forLanguageTag("vi")));

    private final IAddressRepository addressRepository;

    public AddressService(IAddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionResponse> getProvinces() {
        return addressRepository.findProvinces().stream().map(this::toRegion).sorted(BY_NAME).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegionResponse> getWards(String provinceId) {
        return addressRepository.findWardsByProvince(provinceId).stream().map(this::toRegion).sorted(BY_NAME).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses(UUID userId) {
        return addressRepository.findByUser(userId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(UUID userId) {
        return addressRepository.findDefault(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(DEFAULT_ADDRESS_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getAddress(UUID userId, Long addressId) {
        return toResponse(requireOwned(userId, addressId));
    }

    @Override
    @Transactional
    public AddressResponse createAddress(UUID userId, SaveAddressCommand command) {
        Ward ward = requireWard(command.getWardId());
        boolean makeDefault = Boolean.TRUE.equals(command.getIsDefault()) || addressRepository.findDefault(userId).isEmpty();
        if (makeDefault) {
            addressRepository.clearDefault(userId);
        }

        Address address = Address.builder()
                .userId(userId)
                .ward(ward)
                .detail(command.getDetail())
                .isDefault(makeDefault)
                .build();
        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, Long addressId, SaveAddressCommand command) {
        Address address = requireOwned(userId, addressId);
        Ward ward = requireWard(command.getWardId());

        // The default can only be replaced (by making another address the default), never just unset
        if (!address.isDefaultAddress() && Boolean.TRUE.equals(command.getIsDefault())) {
            addressRepository.clearDefault(userId);
            address.setIsDefault(true);
        }

        address.setWard(ward);
        address.setDetail(command.getDetail());
        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse setDefaultAddress(UUID userId, Long addressId) {
        Address address = requireOwned(userId, addressId);
        if (address.isDefaultAddress()) {
            return toResponse(address);
        }
        addressRepository.clearDefault(userId);
        address.setIsDefault(true);
        return toResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, Long addressId) {
        Address address = requireOwned(userId, addressId);
        addressRepository.delete(addressId);

        if (address.isDefaultAddress()) {
            addressRepository.findLatest(userId).ifPresent(next -> {
                next.setIsDefault(true);
                addressRepository.save(next);
            });
        }
    }

    // 404 (rather than 403) for another user's address, so address ids cannot be probed
    private Address requireOwned(UUID userId, Long addressId) {
        return addressRepository.findByIdAndUser(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND));
    }

    private Ward requireWard(String wardId) {
        return addressRepository.findWardById(wardId)
                .orElseThrow(() -> new BadRequestException(WARD_NOT_FOUND));
    }

    private RegionResponse toRegion(Province province) {
        return RegionResponse.builder().id(province.getId()).name(province.getName()).fullName(province.getFullName()).build();
    }

    private RegionResponse toRegion(Ward ward) {
        return RegionResponse.builder().id(ward.getId()).name(ward.getName()).fullName(ward.getFullName()).build();
    }

    private AddressResponse toResponse(Address address) {
        Ward ward = address.getWard();
        Province province = ward != null ? ward.getProvince() : null;
        return AddressResponse.builder()
                .id(address.getId())
                .detail(address.getDetail())
                .isDefault(address.isDefaultAddress())
                .ward(ward != null ? toRegion(ward) : null)
                .province(province != null ? toRegion(province) : null)
                .fullAddress(address.fullAddress())
                .build();
    }
}
