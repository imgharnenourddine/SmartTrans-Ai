package com.smarttrans.ai.assistant.dto;

import java.util.List;
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
public class AssistantAskResponse {

	private String answer;
	private String mode;
	private List<AssistantCitation> citations;
}
