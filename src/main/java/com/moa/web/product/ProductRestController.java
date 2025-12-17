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
            // String fullPath = uploadDir + savedFilename;
            String fullPath = combinePath(uploadDir, savedFilename);

            logger.debug("Saving to: {}", fullPath);

            File dest = new File(fullPath);
            // Ensure directory exists
            if (!dest.getParentFile().exists()) {
                boolean created = dest.getParentFile().mkdirs();
                logger.debug("Created directory: {}, success: {}", dest.getParentFile().getAbsolutePath(), created);
            }

            file.transferTo(dest.getAbsoluteFile());
            logger.debug("File saved successfully");

            // [Modified 2025-12-17] 내부 combinePath 메서드 사용하여 URL 결합
            // return urlPrefix + savedFilename;
            return combinePath(urlPrefix, savedFilename);
        } catch (Exception e) {
            logger.error("File upload failed", e);
            throw e;
        }
    }

    @PostMapping("/upload-images")
    public String uploadImages(
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            @RequestParam(value = "iconFile", required = false) MultipartFile iconFile) throws Exception {
        try {
            // 적어도 하나의 파일은 필요
            boolean hasLogo = logoFile != null && !logoFile.isEmpty();
            boolean hasIcon = iconFile != null && !iconFile.isEmpty();

            if (!hasLogo && !hasIcon) {
                throw new RuntimeException("At least one file (logo or icon) is required");
            }

            // 기본 이름 생성 (UUID 기반)
            String baseName = UUID.randomUUID().toString().substring(0, 8);
            String savedLogoFilename = null;

            // 로고 파일 저장
            if (hasLogo) {
                String logoExtension = getFileExtension(logoFile.getOriginalFilename());
                savedLogoFilename = baseName + "_logo" + logoExtension;
                String logoFullPath = combinePath(uploadDir, savedLogoFilename);

                logger.debug("Saving logo to: {}", logoFullPath);

                File logoDest = new File(logoFullPath);
                ensureDirectoryExists(logoDest);
                logoFile.transferTo(logoDest.getAbsoluteFile());
                logger.debug("Logo file saved successfully");
            }

            // 아이콘 파일 저장
            if (hasIcon) {
                String iconExtension = getFileExtension(iconFile.getOriginalFilename());
                String iconFilename = baseName + "_icon" + iconExtension;
                String iconFullPath = combinePath(uploadDir, iconFilename);

                logger.debug("Saving icon to: {}", iconFullPath);

                File iconDest = new File(iconFullPath);
                ensureDirectoryExists(iconDest);
                iconFile.transferTo(iconDest.getAbsoluteFile());
                logger.debug("Icon file saved successfully");
            }

            // 로고가 있으면 로고 URL 반환, 없으면 아이콘 기반으로 로고 URL 생성
            if (savedLogoFilename != null) {
                return combinePath(urlPrefix, savedLogoFilename);
            } else {
                // 아이콘만 업로드한 경우 - 아이콘 URL 반환 (또는 빈 문자열)
                String iconFilename = baseName + "_icon" + getFileExtension(iconFile.getOriginalFilename());
                return combinePath(urlPrefix, iconFilename);
            }
        } catch (Exception e) {
            logger.error("Image upload failed", e);
            throw e;
        }
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private void ensureDirectoryExists(File file) {
        if (!file.getParentFile().exists()) {
            boolean created = file.getParentFile().mkdirs();
            logger.debug("Created directory: {}, success: {}", file.getParentFile().getAbsolutePath(), created);
        }
    }

    @GetMapping("/categorie")
    public ApiResponse<List<ProductDTO>> getCategoryList() throws Exception {
        logger.debug("Request [getCategoryList] Time: {}", java.time.LocalDateTime.now());
        return ApiResponse.success(productService.getCategoryList());
    }

    private String combinePath(String prefix, String suffix) {
        if (prefix == null)
            prefix = "";
        if (suffix == null)
            suffix = "";

        boolean prefixHasSlash = prefix.endsWith("/") || prefix.endsWith("\\");
        boolean suffixHasSlash = suffix.startsWith("/") || suffix.startsWith("\\");

        if (prefixHasSlash && suffixHasSlash) {
            return prefix + suffix.substring(1);
        } else if (!prefixHasSlash && !suffixHasSlash) {
            return prefix + "/" + suffix;
        } else {
            return prefix + suffix;
        }
    }
}
