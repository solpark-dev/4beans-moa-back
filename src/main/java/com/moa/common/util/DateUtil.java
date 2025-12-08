package com.moa.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static String formatDate(LocalDateTime dateTime) {
		return dateTime == null ? null : DATE.format(dateTime);
	}

	public static String formatDateTime(LocalDateTime dateTime) {
		return dateTime == null ? null : DATE_TIME.format(dateTime);
	}
}
