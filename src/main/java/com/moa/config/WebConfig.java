package com.moa.config;

import java.nio.file.Paths;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@MapperScan(basePackages = "com.moa.dao")
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.user.profile-dir}")
    private String profileUploadDir;

    @Value("${app.upload.user.profile-url-prefix}")
    private String profileUrlPrefix;

    @Value("${app.upload.product-image-dir}")
    private String productImageUploadDir;

    @Value("${app.upload.product-image-url-prefix}")
    private String productImageUrlPrefix;
    
    @Value("${app.upload.community.inquiry-dir}")
    private String communityInquiryDir;

    @Value("${app.upload.community.inquiry-url-prefix}")
    private String communityInquiryUrlPrefix;

    @Value("${app.upload.community.answer-dir}")
    private String communityAnswerDir;

    @Value("${app.upload.community.answer-url-prefix}")
    private String communityAnswerUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler(profileUrlPrefix + "/**")
                .addResourceLocations(Paths.get(profileUploadDir)
                        .toAbsolutePath()
                        .toUri()
                        .toString());

        registry.addResourceHandler(productImageUrlPrefix + "/**")
                .addResourceLocations(Paths.get(productImageUploadDir)
                        .toAbsolutePath()
                        .toUri()
                        .toString());
        
        registry.addResourceHandler(communityInquiryUrlPrefix + "/**")
        .addResourceLocations(Paths.get(communityInquiryDir)
        		.toAbsolutePath()
        		.toUri()
        		.toString());
        
        registry.addResourceHandler(communityAnswerUrlPrefix + "/**")
        .addResourceLocations(Paths.get(communityAnswerDir)
        		.toAbsolutePath()
        		.toUri()
        		.toString());
    }

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {

		registry.addViewController("/{path:^(?!api|oauth|uploads|assets|css|js|images|favicon\\.ico|index\\.html).*$}")
				.setViewName("index.html");
	}
}