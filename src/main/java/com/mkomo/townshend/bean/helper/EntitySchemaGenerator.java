package com.mkomo.townshend.bean.helper;

import java.util.List;

import com.mkomo.townshend.bean.helper.json.JsonSchema;

public class EntitySchemaGenerator {

	public static JsonSchema getSchema(Class<?> cls, List<Class<?>> classesToReference) {
		return JsonSchema.schemaFromClass(cls, classesToReference, false);
	}
}
