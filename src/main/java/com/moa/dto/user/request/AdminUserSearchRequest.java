package com.moa.dto.user.request;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserSearchRequest {

	private Integer page;
	private Integer size;
	private String q;
	private String status;
	private LocalDate regDateFrom;
	private LocalDate regDateTo;

	private Integer offset;

	public int getPageOrDefault() {
		return page == null || page < 1 ? 1 : page;
	}

	public int getSizeOrDefault() {
		return size == null || size < 1 ? 10 : size;
	}
}
