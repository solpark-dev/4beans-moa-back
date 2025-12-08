package com.moa.domain;

import lombok.Data;

@Data
public class Product {
    private int productId;
    private int categoryId;
    private String productName;
    private String productStatus; // 'ACTIVE' default
    private int price;
    private String image;
    private String categoryName;
}
