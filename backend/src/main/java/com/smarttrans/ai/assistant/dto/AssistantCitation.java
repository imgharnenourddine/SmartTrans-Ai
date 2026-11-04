package com.smarttrans.ai.assistant.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssistantCitation {

	private String kind;
	private UUID id;
	private String title;
	private String snippet;
}
