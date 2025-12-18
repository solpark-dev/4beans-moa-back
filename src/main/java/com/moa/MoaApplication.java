package com.moa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.moa.common.util.EncryptUtil;

@SpringBootApplication
public class MoaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MoaApplication.class, args);
		String raw = "1234";
		String encoded = EncryptUtil.encode(raw);
		
		System.out.println(encoded);
		System.out.println(EncryptUtil.matches("1234", encoded)); // true
		System.out.println(EncryptUtil.matches("12345", encoded)); 
	}
}
