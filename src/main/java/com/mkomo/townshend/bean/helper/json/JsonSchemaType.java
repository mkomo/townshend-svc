package com.mkomo.townshend.bean.helper.json;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonSchemaType {
	string, integer, number, object, array, _boolean, _null;

	private static Map<String, JsonSchemaType> FOR_VALUE_MAP = new LinkedHashMap<>();
	static {
		for (JsonSchemaType value : values()) {
			FOR_VALUE_MAP.put(value.toString(), value);
		}
	}

	@JsonCreator
	public static JsonSchemaType forValue(String value) {
		return FOR_VALUE_MAP.get(value);
	}

	private final String name;

	private JsonSchemaType() {
		this(null);
	}

	private JsonSchemaType(String name) {
		if (name == null) {
			this.name = this.name().replaceAll("^_", "");
		} else {
			this.name = name;
		}
	}

	@JsonValue
	@Override
	public String toString() {
		return this.name;
	}
}
