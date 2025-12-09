package com.moa.dto.user.request;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

	private String sort;
	private List<String> dbSortList;

	private Integer offset;

	public int getPageOrDefault() {
		return page == null || page < 1 ? 1 : page;
	}

	public int getSizeOrDefault() {
		return size == null || size < 1 ? 10 : size;
	}

	public void setSort(String sort) {
		this.sort = sort;

		if (sort == null || sort.isBlank()) {
			this.dbSortList = null;
			return;
		}

		String[] tokens = sort.split(",");
		List<String> list = new ArrayList<>();

		for (int i = 0; i + 1 < tokens.length; i += 2) {
			String field = tokens[i].trim();
			String dir = tokens[i + 1].trim();

			String column = null;
			if ("lastLoginDate".equals(field)) {
				column = "u.LAST_LOGIN_DATE";
			} else if ("regDate".equals(field)) {
				column = "u.REG_DATE";
			} else {
				continue;
			}

			String direction = "ASC".equalsIgnoreCase(dir) ? "ASC" : "DESC";

			list.add(column + " " + direction);
		}

		this.dbSortList = list.isEmpty() ? null : list;
	}

	public void applyPaging() {
		int p = getPageOrDefault();
		int s = getSizeOrDefault();
		this.offset = (p - 1) * s;
		this.size = s;
		this.page = p;
	}
}
