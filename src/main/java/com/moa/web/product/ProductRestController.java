package com.moa.web.product;

import com.moa.dto.product.ProductDTO;
import com.moa.service.product.ProductService;
import com.moa.common.exception.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.util.UUID;

@RestController
@RequestMapping("/api/product")
public class ProductRestController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ProductService productService;

    @Value("${app.upload.product-image-dir}")
    private String uploadDir;

    @Value("${app.upload.product-image-url-prefix}")
    private String urlPrefix;

    @PostMapping
    public ApiResponse<Void> addProduct(@RequestBody ProductDTO productDTO) throws Exception {
        logger.debug("Request [addProduct] Time: {}, Content: {}", java.time.LocalDateTime.now(), productDTO);
        productService.addProduct(productDTO);
        return ApiResponse.success(null);
    }

    /*
     * @GetMapping("/{productId}")
     * public ProductDTO getProduct(@PathVariable int productId) throws Exception {
     * logger.debug("Request [getProduct] Time: {}, productId: {}",
     * java.time.LocalDateTime.now(), productId);
     * return productService.getProduct(productId);
     * }
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductDTO> getProduct(@PathVariable int productId) throws Exception {
        logger.debug("Request [getProduct] Time: {}, productId: {}", java.time.LocalDateTime.now(), productId);
        return ApiResponse.success(productService.getProduct(productId));
    }

    @GetMapping
    public ApiResponse<List<ProductDTO>> getProductList() throws Exception {
        logger.debug("Request [getProductList] Time: {}", java.time.LocalDateTime.now());
        return ApiResponse.success(productService.getProductList());
    }

    @PutMapping
    public ApiResponse<Void> updateProduct(@RequestBody ProductDTO productDTO) throws Exception {
        logger.debug("Request [updateProduct] Time: {}, Content: {}", java.time.LocalDateTime.now(), productDTO);
        productService.updateProduct(productDTO);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> deleteProduct(@PathVariable int productId) throws Exception {
        logger.debug("Request [deleteProduct] Time: {}, productId: {}", java.time.LocalDateTime.now(), productId);
        productService.deleteProduct(productId);
        return ApiResponse.success(null);
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            String originalFilename = file.getOriginalFilename();
            logger.debug("Uploading file: {}", originalFilename);

            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            String savedFilename = UUID.randomUUID().toString() + extension;
            String fullPath = uploadDir + savedFilename;

            logger.debug("Saving to: {}", fullPath);

            File dest = new File(fullPath);
            // Ensure directory exists
            if (!dest.getParentFile().exists()) {
                boolean created = dest.getParentFile().mkdirs();
                logger.debug("Created directory: {}, success: {}", dest.getParentFile().getAbsolutePath(), created);
            }

            file.transferTo(dest.getAbsoluteFile());
            logger.debug("File saved successfully");

            return urlPrefix + savedFilename;
        } catch (Exception e) {
            logger.error("File upload failed", e);
            throw e;
        }
    }

    @GetMapping("/categorie")
    public ApiResponse<List<ProductDTO>> getCategoryList() throws Exception {
        logger.debug("Request [getCategoryList] Time: {}", java.time.LocalDateTime.now());
        return ApiResponse.success(productService.getCategoryList());
    }
}
