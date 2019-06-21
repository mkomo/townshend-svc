package com.mkomo.townshend.bean.helper.json;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum JsonSchemaValidation {
	_enum,
	_const,
	items,
	format,
	title,
	description,
	at_schema(JsonSchema.SCHEMA_KEY),//used when referencing another schema
	debug("$debug"),
	at_autogenerated("@autogenerated");

	private static Map<String, JsonSchemaValidation> FOR_VALUE_MAP = new LinkedHashMap<>();
	static {
		for (JsonSchemaValidation value : values()) {
			FOR_VALUE_MAP.put(value.toString(), value);
		}
	}

	@JsonCreator
	public static JsonSchemaValidation forValue(String value) {
		return FOR_VALUE_MAP.get(value);
	}

	private final String name;

	private JsonSchemaValidation() {
		this(null);
	}

	private JsonSchemaValidation(String name) {
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
