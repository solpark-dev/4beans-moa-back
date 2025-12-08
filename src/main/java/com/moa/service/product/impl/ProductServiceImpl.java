package com.moa.service.product.impl;

import com.moa.dao.product.ProductDao;
import com.moa.domain.Product;
import com.moa.dto.product.ProductDTO;
import com.moa.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDao productDao;

    @Override
    public void addProduct(ProductDTO productDTO) throws Exception {
        productDao.addProduct(productDTO.toEntity());
    }

    @Override
    public ProductDTO getProduct(int productId) throws Exception {
        Product product = productDao.getProduct(productId);
        return ProductDTO.fromEntity(product);
    }

    @Override
    public List<ProductDTO> getProductList() throws Exception {
        List<Product> productList = productDao.getProductList();
        return productList.stream()
                .map(ProductDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void updateProduct(ProductDTO productDTO) throws Exception {
        productDao.updateProduct(productDTO.toEntity());
    }

    @Override
    public void deleteProduct(int productId) throws Exception {
        productDao.deleteProduct(productId);
    }

    @Override
    public List<ProductDTO> getCategoryList() throws Exception {
        List<Product> categories = productDao.getCategoryList();
        return categories.stream()
                .map(this::convertToCategoryDTO)
                .collect(Collectors.toList());
    }

    private ProductDTO convertToCategoryDTO(Product category) {
        ProductDTO dto = new ProductDTO();
        dto.setCategoryId(category.getCategoryId());
        dto.setCategoryName(category.getCategoryName());
        return dto;
    }
}
