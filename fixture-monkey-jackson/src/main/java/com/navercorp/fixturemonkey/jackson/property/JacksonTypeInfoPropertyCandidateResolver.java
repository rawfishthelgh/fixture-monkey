/*
 * Fixture Monkey
 *
 * Copyright (c) 2021-present NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.fixturemonkey.jackson.property;

import static com.navercorp.fixturemonkey.jackson.property.JacksonAnnotations.getJacksonAnnotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.navercorp.fixturemonkey.api.property.ElementProperty;
import com.navercorp.fixturemonkey.api.property.Property;
import com.navercorp.fixturemonkey.api.property.PropertyCandidateResolver;
import com.navercorp.fixturemonkey.api.type.Types;

@API(since = "1.1.0", status = Status.EXPERIMENTAL)
public final class JacksonTypeInfoPropertyCandidateResolver implements PropertyCandidateResolver {
	@Override
	public List<Property> resolveCandidateProperties(Property property) {
		JsonSubTypes jsonSubTypes;
		JsonTypeInfo jsonTypeInfo;
		if (property instanceof ElementProperty
			&& getJacksonAnnotation(((ElementProperty)property).getContainerProperty(), JsonSubTypes.class) != null) {
			Property containerProperty = ((ElementProperty)property).getContainerProperty();
			jsonSubTypes = getJacksonAnnotation(containerProperty, JsonSubTypes.class);
			jsonTypeInfo = getJacksonAnnotation(containerProperty, JsonTypeInfo.class);
		} else {
			jsonSubTypes = getJacksonAnnotation(property, JsonSubTypes.class);
			if (jsonSubTypes == null) {
				throw new IllegalArgumentException("@JsonSubTypes is not found " + property.getType().getTypeName());
			}

			boolean notSubTypes = Arrays.stream(jsonSubTypes.value())
				.map(JsonSubTypes.Type::value)
				.noneMatch(type -> Types.isAssignable(type, Types.getActualType(property.getType())));

			if (notSubTypes) {
				return Collections.singletonList(property);
			}

			jsonTypeInfo = getJacksonAnnotation(property, JsonTypeInfo.class);
		}

		List<Annotation> annotations = new ArrayList<>(property.getAnnotations());
		annotations.add(jsonTypeInfo);

		return Arrays.stream(Objects.requireNonNull(jsonSubTypes).value())
			.map(it -> new Property() {
				@Override
				public Type getType() {
					return it.value();
				}

				@Override
				public AnnotatedType getAnnotatedType() {
					return Types.generateAnnotatedTypeWithoutAnnotation(it.value());
				}

				@Nullable
				@Override
				public String getName() {
					return property.getName();
				}

				@Override
				public List<Annotation> getAnnotations() {
					return annotations;
				}

				@Nullable
				@Override
				public Object getValue(Object instance) {
					return property.getValue(instance);
				}

				@Override
				public int hashCode() {
					return getType().hashCode();
				}

				@Override
				public boolean equals(Object obj) {
					if (this == obj) {
						return true;
					}
					if (obj == null || Types.isAssignable(getClass(), obj.getClass())) {
						return false;
					}

					Property that = (Property)obj;
					return getType().equals(that.getType());
				}
			})
			.collect(Collectors.toList());
	}
}
