package com.moa.config;

import java.nio.file.Paths;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@MapperScan(basePackages = "com.moa.dao")
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.upload.product-image-dir}")
	private String productImageUploadDir;

	@Value("${app.upload.user.profile-dir}")
	private String profileUploadDir;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		registry.addResourceHandler("/uploads/user/profile/**")
				.addResourceLocations(Paths.get(profileUploadDir).toAbsolutePath().toUri().toString());

		registry.addResourceHandler("/uploads/product-image/**")
				.addResourceLocations(Paths.get(productImageUploadDir).toAbsolutePath().toUri().toString());
	}
}