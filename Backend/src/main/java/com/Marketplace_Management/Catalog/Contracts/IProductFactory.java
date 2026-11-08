package com.Marketplace_Management.Catalog.Contracts;

import java.io.IOException;

import com.Marketplace_Management.Catalog.DTOs.Commands.Product.CreateProductCommand;
import com.Marketplace_Management.Catalog.Entities.Product;

public interface IProductFactory {

    Product create(CreateProductCommand command) throws IOException;
    
} 
