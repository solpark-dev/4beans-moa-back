package com.moa.web.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.moa.domain.Product;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j  // log를 사용하려면 Lombok의 @Slf4j 어노테이션을 테스트 클래스에 추가
class ProductRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static Product testProduct;
    private static Integer insertedProductId; 

    @BeforeAll
    static void setUpAll() {
        testProduct = new Product();
        //testProduct.setProductId(20);
        testProduct.setCategoryId(1); // 실제 존재하는 카테고리 ID로 수정 필요
        testProduct.setProductName("테스트 상품");
        testProduct.setProductStatus("ACTIVE");
        testProduct.setPrice(50000);
        testProduct.setImage("test_product.jpg");
    }

    @Test
    @Order(1)
    @DisplayName("상품 등록 테스트")
    @Rollback(false)  // 이거 추가! @Transactional  // 이 어노테이션 때문!테스트가 끝나면 자동으로 롤백됩니다.
    void testAddProduct() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProduct)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();
        
        // 등록 후 목록 조회해서 방금 들어간 상품 찾기
        MvcResult listResult = mockMvc.perform(get("/api/product"))
                .andExpect(status().isOk())
                .andReturn();
        
        String json = listResult.getResponse().getContentAsString();
        // 첫 번째 상품(가장 최근)의 ID 저장
        insertedProductId = JsonPath.parse(json).read("$[0].productId");
        log.info("Inserted productId: {}", insertedProductId);
    }



    @Test
    @Order(2)
    @DisplayName("상품 단건 조회 테스트")
    void testGetProduct() throws Exception {
        mockMvc.perform(get("/api/product/" + insertedProductId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(insertedProductId))
                .andExpect(jsonPath("$.productName").value("테스트 상품"))
                .andExpect(jsonPath("$.price").value(50000));
    }


    @Test
    @Order(3)
    @DisplayName("상품 목록 조회 테스트")
    void testGetProductList() throws Exception {
        mockMvc.perform(get("/api/product"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @Order(4)
    @DisplayName("상품 수정 테스트")
    @Rollback(false)
    void testUpdateProduct() throws Exception {
        Product updateProduct = new Product();
        updateProduct.setProductId(insertedProductId);  // 등록한 상품 ID 사용
        updateProduct.setCategoryId(1);
        updateProduct.setProductName("수정된 상품명");
        updateProduct.setProductStatus("ACTIVE");
        updateProduct.setPrice(60000);
        updateProduct.setImage("updated_product.jpg");
        
        mockMvc.perform(put("/api/product")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateProduct)))
                .andDo(print())
                .andExpect(status().isOk());
        
        // 수정 확인
        mockMvc.perform(get("/api/product/" + insertedProductId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("수정된 상품명"))
                .andExpect(jsonPath("$.price").value(60000));
    }
    
    @Test
    @Order(5)
    @DisplayName("상품 삭제 테스트")
    @Rollback(false)
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/product/" + insertedProductId))
                .andDo(print())
                .andExpect(status().isOk());
        
        // 목록에서 해당 상품이 없는지 확인
        mockMvc.perform(get("/api/product"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == " + insertedProductId + ")]").doesNotExist());
    }
}
