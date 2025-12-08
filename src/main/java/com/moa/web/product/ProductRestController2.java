package com.moa.web.product;

import com.moa.dto.product.ProductDTO;
import com.moa.service.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@RestController
//@RequestMapping("/api/product")
public class ProductRestController2 {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ProductService productService;

    @PostMapping
    public void addProduct(@RequestBody ProductDTO productDTO) throws Exception {
        logger.debug("Request [addProduct] Time: {}, Content: {}", java.time.LocalDateTime.now(), productDTO);
        productService.addProduct(productDTO);
    }

    @GetMapping("/{productId}")
    public ProductDTO getProduct(@PathVariable int productId) throws Exception {
        logger.debug("Request [getProduct] Time: {}, productId: {}", java.time.LocalDateTime.now(), productId);
        return productService.getProduct(productId);
    }

    @GetMapping
    public List<ProductDTO> getProductList() throws Exception {
        logger.debug("Request [getProductList] Time: {}", java.time.LocalDateTime.now());
        return productService.getProductList();
    }

    @PutMapping
    public void updateProduct(@RequestBody ProductDTO productDTO) throws Exception {
        logger.debug("Request [updateProduct] Time: {}, Content: {}", java.time.LocalDateTime.now(), productDTO);
        productService.updateProduct(productDTO);
    }

    @DeleteMapping("/{productId}")
    public void deleteProduct(@PathVariable int productId) throws Exception {
        logger.debug("Request [deleteProduct] Time: {}, productId: {}", java.time.LocalDateTime.now(), productId);
        productService.deleteProduct(productId);
    }
}
