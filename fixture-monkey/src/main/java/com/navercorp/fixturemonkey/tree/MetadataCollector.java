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

package com.navercorp.fixturemonkey.tree;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;

import com.navercorp.fixturemonkey.api.property.Property;

@API(since = "0.4.0", status = Status.MAINTAINED)
final class MetadataCollector {
	private final ObjectNode rootNode;
	private final Map<Property, List<ObjectNode>> nodesByProperty;

	public MetadataCollector(ObjectNode rootNode) {
		this.rootNode = rootNode;
		this.nodesByProperty = new LinkedHashMap<>();
	}

	public ObjectTreeMetadata collect() {
		for (ObjectNode child : rootNode.resolveChildren()) {
			collect(child);
		}
		return new ObjectTreeMetadata(Collections.unmodifiableMap(nodesByProperty));
	}

	private void collect(ObjectNode node) {
		Property property = node.getTreeProperty().getObjectProperty().getProperty();

		List<ObjectNode> children = node.resolveChildren();
		for (ObjectNode child : children) {
			collect(child);
		}

		List<ObjectNode> list = Collections.singletonList(node);
		nodesByProperty.merge(
			property,
			list,
			(prev, now) -> Stream.concat(prev.stream(), now.stream()).collect(Collectors.toList())
		);
	}

}
