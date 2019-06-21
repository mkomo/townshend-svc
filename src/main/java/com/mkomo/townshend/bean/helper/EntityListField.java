package com.mkomo.townshend.bean.helper;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class EntityListField<T> extends ArrayList<T> {

	public EntityListField(List<T> info) {
		super(info);
	}

	private static final long serialVersionUID = -7035752612529895779L;

	public static class EntityListConverter implements AttributeConverter<EntityListField<?>, String> {

		private final static ObjectMapper MAPPER = new ObjectMapper();

		@Override
		public String convertToDatabaseColumn(EntityListField<?> meta) {
			try {
				String json = MAPPER.writeValueAsString(meta);
				return json;
			} catch (JsonProcessingException ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public EntityListField<?> convertToEntityAttribute(String dbData) {
			if (dbData == null) {
				return null;
			}
			try {
				EntityListField<?> deserializedList =
						MAPPER.readValue(dbData, new TypeReference<EntityListField<?>>() {});
				return deserializedList;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

	}
}
