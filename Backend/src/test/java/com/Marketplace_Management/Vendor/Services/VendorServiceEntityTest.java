package com.Marketplace_Management.Vendor.Services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.Marketplace_Management.Shared.Contracts.IEventPublisher;
import com.Marketplace_Management.Shared.Contracts.IFileService;
import com.Marketplace_Management.Shared.Errors.Exceptions.ResourceNotFoundException;
import com.Marketplace_Management.Shared.Events.EventOptions;
import com.Marketplace_Management.Vendor.Constants.VendorStatus;
import com.Marketplace_Management.Vendor.Contracts.IVendorRepository;
import com.Marketplace_Management.Vendor.DTOs.Command.UpdateCollectionCommand;
import com.Marketplace_Management.Vendor.DTOs.Command.VendorProfileCommand;
import com.Marketplace_Management.Vendor.Entities.CollectionEntity;
import com.Marketplace_Management.Vendor.Entities.VendorEntity;
import com.Marketplace_Management.Vendor.Events.VendorActivatedEvent;
import com.Marketplace_Management.Vendor.Repositories.CollectionJpaRepository;

@ExtendWith(MockitoExtension.class)
class VendorServiceEntityTest {
    @Mock IVendorRepository repository;
    @Mock CollectionJpaRepository collections;
    @Mock IEventPublisher events;
    @Mock IFileService files;
    @InjectMocks VendorService service;

    @Test
    void activationKeepsItsRuleAndPublishesOnlyTheOwnerId() {
        VendorEntity vendor = VendorEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID()).build();
        assertEquals(VendorStatus.PENDING, vendor.getStatus());
        when(repository.findById(vendor.getId())).thenReturn(Optional.of(vendor));
        when(repository.save(vendor)).thenReturn(vendor);

        service.active(vendor.getId());

        assertEquals(VendorStatus.ACTIVE, vendor.getStatus());
        var event = ArgumentCaptor.forClass(VendorActivatedEvent.class);
        verify(events).publish(event.capture(), any(EventOptions.class));
        assertEquals(vendor.getUserId(), event.getValue().getUserId());
    }

    @Test
    void alreadyActiveVendorCannotBeActivatedAgain() {
        VendorEntity vendor = VendorEntity.builder().id(UUID.randomUUID()).status(VendorStatus.ACTIVE).build();
        when(repository.findById(vendor.getId())).thenReturn(Optional.of(vendor));

        assertThrows(IllegalStateException.class, () -> service.active(vendor.getId()));

        verify(repository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void collectionUpdateKeepsTheEntityOwnerAndCreationTimestamp() {
        VendorEntity vendor = VendorEntity.builder().id(UUID.randomUUID()).build();
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 12, 0);
        CollectionEntity collection = CollectionEntity.builder().id(UUID.randomUUID()).vendor(vendor)
                .name("Original").createdAt(created).displayOrder(1).build();
        when(collections.findById(collection.getId())).thenReturn(Optional.of(collection));
        when(collections.save(collection)).thenReturn(collection);
        var command = new UpdateCollectionCommand();
        command.setName("Updated");
        command.setDisplayOrder(2);

        var response = service.updateCollection(vendor.getId(), collection.getId(), command);

        assertSame(vendor, collection.getVendor());
        assertEquals(created, collection.getCreatedAt());
        assertEquals("Updated", response.getName());
        assertEquals(2, response.getDisplayOrder());
    }

    @Test
    void collectionFromAnotherVendorIsRejected() {
        CollectionEntity collection = CollectionEntity.builder().id(UUID.randomUUID())
                .vendor(VendorEntity.builder().id(UUID.randomUUID()).build()).build();
        when(collections.findById(collection.getId())).thenReturn(Optional.of(collection));

        assertThrows(ResourceNotFoundException.class, () -> service.updateCollection(
                UUID.randomUUID(), collection.getId(), new UpdateCollectionCommand()));

        verify(collections, never()).save(any());
    }

    @Test
    void profileUpdatePreservesStatusOwnerAndImagesWhenNoFilesAreSent() throws Exception {
        VendorEntity vendor = VendorEntity.builder().id(UUID.randomUUID()).userId(UUID.randomUUID())
                .status(VendorStatus.ACTIVE).logo("uploads/logo.png").banner("uploads/banner.png").build();
        when(repository.findById(vendor.getId())).thenReturn(Optional.of(vendor));
        when(repository.save(vendor)).thenReturn(vendor);

        service.update(vendor.getId(), VendorProfileCommand.builder().name("Updated store").build());

        assertEquals(VendorStatus.ACTIVE, vendor.getStatus());
        assertEquals("uploads/logo.png", vendor.getLogo());
        assertEquals("uploads/banner.png", vendor.getBanner());
        verify(repository).save(vendor);
        verifyNoInteractions(files, events);
    }
}
