package com.moa.domain;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatKnowledge {

	private Long id;
	private String category;
	private String title;
	private String question;
	private String answer;
	private String keywords;
	private String embedding;
	private LocalDateTime updatedAt;
}
