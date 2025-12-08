package com.moa.service.product;

import com.moa.dto.product.ProductDTO;
import java.util.List;

public interface ProductService {

    public void addProduct(ProductDTO productDTO) throws Exception;

    public ProductDTO getProduct(int productId) throws Exception;

    public List<ProductDTO> getProductList() throws Exception;

    public void updateProduct(ProductDTO productDTO) throws Exception;

    public void deleteProduct(int productId) throws Exception;

    public List<ProductDTO> getCategoryList() throws Exception;
}
