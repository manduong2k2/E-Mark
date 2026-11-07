package com.Marketplace_Management.Vendor.Services;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.Marketplace_Management.Shared.Configuration.RabbitMqQueues.VendorQueueConfig;
import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Shared.Security.SecurityUtils;
import com.Marketplace_Management.Vendor.Contracts.IVendorRepository;
import com.Marketplace_Management.Vendor.Contracts.IVendorService;
import com.Marketplace_Management.Vendor.DTOs.Command.CreateCollectionCommand;
import com.Marketplace_Management.Vendor.DTOs.Command.GetListVendorCommand;
import com.Marketplace_Management.Vendor.DTOs.Command.UpdateCollectionCommand;
import com.Marketplace_Management.Vendor.DTOs.Command.VendorProfileCommand;
import com.Marketplace_Management.Vendor.DTOs.Response.CollectionResponse;
import com.Marketplace_Management.Vendor.DTOs.Response.VendorResponse;
import com.Marketplace_Management.Vendor.Entities.CollectionEntity;
import com.Marketplace_Management.Vendor.Events.VendorActivatedEvent;
import com.Marketplace_Management.Vendor.Entities.VendorEntity;
import com.Marketplace_Management.Vendor.Constants.VendorStatus;
import com.Marketplace_Management.Vendor.Repositories.CollectionJpaRepository;

@Service
public class VendorService implements IVendorService {
    private static final String VENDOR_NOT_FOUND = "Vendor not found";

    private final IVendorRepository vendorRepository;
    private final CollectionJpaRepository collectionJpaRepository;
    private final IEventPublisher eventPublisher;
    private final IFileService fileService;

    @Value ("${spring.application.base-url}")
    private String baseUrl;

    public VendorService(IVendorRepository vendorRepository, CollectionJpaRepository collectionJpaRepository,
            IEventPublisher eventPublisher, IFileService fileService) {
        this.vendorRepository = vendorRepository;
        this.collectionJpaRepository = collectionJpaRepository;
        this.eventPublisher = eventPublisher;
        this.fileService = fileService;
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> getAll(GetListVendorCommand command) {
        return vendorRepository.findAll(command).stream()
                .map(vendor -> vendor.withUrl(baseUrl))
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorResponse getById(UUID vendorId) {
        return new VendorResponse(requireVendor(vendorId)).withUrl(baseUrl);
    }

    @Transactional(readOnly = true)
    public List<CollectionResponse> getCollections(UUID vendorId) {
        var collections = collectionJpaRepository.findByVendorId(vendorId);
        return collections.stream()
                .map(CollectionResponse::new)
                .toList();
    }

    @Transactional
    public CollectionResponse createCollection(UUID vendorId, CreateCollectionCommand command) {
        CollectionEntity collection = CollectionEntity.builder()
                .vendor(requireVendor(vendorId))
                .name(command.getName())
                .displayOrder(command.getDisplayOrder())
                .build();
        return new CollectionResponse(collectionJpaRepository.save(collection));
    }

    @Transactional
    public CollectionResponse updateCollection(UUID vendorId, UUID collectionId, UpdateCollectionCommand command) {
        CollectionEntity collection = requireCollectionOfVendor(vendorId, collectionId);
        collection.setName(command.getName());
        collection.setDisplayOrder(command.getDisplayOrder());
        return new CollectionResponse(collectionJpaRepository.save(collection));
    }

    @Transactional
    public void removeCollection(UUID vendorId, UUID collectionId) {
        requireCollectionOfVendor(vendorId, collectionId);
        collectionJpaRepository.deleteById(collectionId);
    }

    /** Admin: creates the vendor of any user. */
    @Transactional(rollbackFor = IOException.class)
    public VendorResponse create(UUID userId, VendorProfileCommand command) throws IOException {
        if (vendorRepository.existsByUserId(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor of this user already exists");
        }

        VendorEntity vendor = VendorEntity.builder()
                .userId(userId)
                .name(command.getName())
                .status(VendorStatus.PENDING)
                .description(command.getDescription())
                .logo(uploadIfPresent(command.getLogo(), "vendors/logo"))
                .banner(uploadIfPresent(command.getBanner(), "vendors/banner"))
                .taxCode(command.getTaxCode())
                .email(command.getEmail())
                .addressId(command.getAddressId())
                .phone(command.getPhone())
                .build();

        vendor = vendorRepository.save(vendor);
        return new VendorResponse(vendor);
    }

    /** The current user registers their own vendor (pending until an admin activates it). */
    @Transactional(rollbackFor = IOException.class)
    public VendorResponse register(VendorProfileCommand command) throws IOException {
        return create(SecurityUtils.currentUserId(), command);
    }

    @Transactional
    public void active(UUID vendorId) {
        VendorEntity vendor = requireVendor(vendorId);
        vendor.activate();
        vendor = vendorRepository.save(vendor);
        // Auth grants the VENDOR role to the owner
        eventPublisher.publish(new VendorActivatedEvent(vendor.getUserId()), new EventOptions(VendorQueueConfig.VENDOR_ACTIVATED_QUEUE, false));
    }

    @Transactional
    public void ban(UUID vendorId) {
        VendorEntity vendor = requireVendor(vendorId);
        vendor.ban();
        vendor = vendorRepository.save(vendor);
    }

    /** Updates the profile; logo / banner are replaced only when a new file is uploaded. */
    @Transactional(rollbackFor = IOException.class)
    public void update(UUID vendorId, VendorProfileCommand command) throws IOException {
        VendorEntity vendor = requireVendor(vendorId);

        vendor.setName(command.getName());
        vendor.setDescription(command.getDescription());
        vendor.setTaxCode(command.getTaxCode());
        vendor.setEmail(command.getEmail());
        vendor.setAddressId(command.getAddressId());
        vendor.setPhone(command.getPhone());
        String logo = uploadIfPresent(command.getLogo(), "vendors/logo");
        if (logo != null) {
            vendor.setLogo(logo);
        }
        String banner = uploadIfPresent(command.getBanner(), "vendors/banner");
        if (banner != null) {
            vendor.setBanner(banner);
        }

        vendor = vendorRepository.save(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponse getByUser(UUID userId) {
        return vendorRepository.findByUserId(userId)
                .map(vendor -> new VendorResponse(vendor).withUrl(baseUrl))
                .orElse(null);
    }

    private VendorEntity requireVendor(UUID vendorId) {
        return vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(VENDOR_NOT_FOUND));
    }

    private String uploadIfPresent(MultipartFile file, String folder) throws IOException {
        return file != null && !file.isEmpty() ? fileService.uploadFile(file, folder) : null;
    }

    // A collection id from the URL must belong to the vendor in the URL (which the caller is allowed to manage)
    private CollectionEntity requireCollectionOfVendor(UUID vendorId, UUID collectionId) {
        return collectionJpaRepository.findById(collectionId)
                .filter(c -> c.getVendor() != null && vendorId.equals(c.getVendor().getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found"));
    }
}
