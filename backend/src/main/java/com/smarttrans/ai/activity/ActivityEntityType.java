package com.smarttrans.ai.activity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ActivityEntityType {

	public static final String MISSION = "MISSION";
	public static final String DRIVER = "DRIVER";
	public static final String VEHICLE = "VEHICLE";
	public static final String CLIENT = "CLIENT";
	public static final String USER = "USER";

	private ActivityEntityType() {
	}

	public static Map<String, Object> metadata(Object... keyValues) {
		if (keyValues.length % 2 != 0) {
			throw new IllegalArgumentException("metadata keys and values must be paired");
		}
		Map<String, Object> metadata = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			Object value = keyValues[i + 1];
			if (value != null) {
				metadata.put(String.valueOf(keyValues[i]), value);
			}
		}
		return metadata.isEmpty() ? null : metadata;
	}
}
