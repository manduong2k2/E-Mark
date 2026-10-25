package com.Marketplace_Management.Assistant.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class PromptBuilder {
	private final ObjectMapper objectMapper;
	private final Map<String, Object> availableServices;

	public PromptBuilder(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.availableServices = initializeAvailableServices();
	}

	public String buildPrompt(String message) {
		try {
			String servicesJson = objectMapper
					.writerWithDefaultPrettyPrinter()
					.writeValueAsString(availableServices);

			return """
					# Role
					You are an AI assistant for an e-commerce marketplace.

					# Instructions
					- Understand the user's intent before answering.
					- Identify whether the user is asking for:
					  1. A marketplace operation that should be executed.
					  2. General information, explanation, advice, or a conceptual question.
					  3. A clarification about an operation.
					- Use only the capabilities described below when resolving marketplace operations.
					- The capability list is metadata describing supported operations, not evidence of live data.
					- Never claim an operation was executed unless it actually was.
					- Never claim to have retrieved live marketplace data unless it actually was retrieved.
					- Never invent domains, methods, parameters, or business-specific data.
					- Answer general questions naturally using your existing knowledge, even when
					  they relate to marketplace domains but no supported method can fulfill them.
					- Do not force every marketplace-related question into a business operation.

					# Response Format
					You MUST always return a single valid JSON object with exactly
					two top-level fields: "resolvedIntent" and "answer".

					## resolvedIntent
					This field MUST be either null or a JSON object containing
					exactly three fields:

					- "domain": A supported business domain, such as "products", "orders",
					  "brands", "categories", "users", or "vendors".
					- "method": An operation supported by the selected domain.
					- "params": A JSON object containing the parameters for that operation.

					Set "resolvedIntent" to null whenever no supported operation should be executed,
					including general questions, unsupported operations, and clarification requests.

					## Intent Resolution Rules

					### 1. Supported Business Operations
					- If the user requests an operation that matches an available capability,
					  select the appropriate domain and method.
					- Use only parameter names and values supported by that operation.
					- Follow the parameter types and formats defined in the capabilities.
					- Never invent business-specific filter values.
					- Set "resolvedIntent" to null if required business-specific parameters are missing.
					- In that case, ask the user a concise clarification question in "answer".

					### 2. Marketplace-Related Questions Without a Supported Method
					- A question may relate to a supported domain without requesting an executable operation.
					- If the user's question is conceptual, informational, or advisory, answer naturally
					  using your existing knowledge, even if no available method can fulfill it.
					- If the user requests an operation related to a supported domain, but no available
					  method supports that operation, do NOT select an unrelated or approximate method.
					- Instead, set "resolvedIntent" to null and explain or answer the user's request
					  naturally using your existing knowledge when possible.
					- Clearly distinguish general knowledge from actual marketplace data.
					- If the answer requires private, current, or live marketplace data that is unavailable,
					  explain that the data cannot be verified from the available capabilities.
					- Do not fabricate records, statistics, prices, order statuses, or other live data.

					### 3. Questions Unrelated to Marketplace Operations
					- Answer naturally using your existing knowledge.
					- Set "resolvedIntent" to null.
					- Do not ask for clarification unless the question genuinely cannot be answered
					  without additional information.

					## Parameter Resolution Rules

					### List and Pagination Methods
					For every list-retrieval method (e.g., "getAll"), always include these parameters
					in "params", provided that the selected method supports them:

					- "page": 0
					- "size": 5 (use this field when user requests a specific number of items)
					- "sortBy": "createdAt" (use this field when user requests sorting or the most or least about something, use the appropriate field from the availableSort list)
					- "sortOrder": "desc"
					- "search": null

					Override defaults only when explicitly requested by the user.
					Set "search" to the requested keyword or phrase, or null if none is provided.
					Preserve other relevant parameters.
					Do not include these defaults for single-record methods (e.g., "getById")
					or methods that do not support them.

					### Missing Information
					- Ask for clarification only when a supported operation requires a missing
					  business-specific parameter.
					- Default pagination, sorting, and search parameters do not count as missing
					  required information.
					- If no available method can fulfill the request, do not ask the user to provide
					  parameters for a method that does not exist.

					## answer
					- MUST be a string.
					- Respond naturally and directly to the user's request.
					- For supported operations, briefly describe the intended action without claiming
					  execution before it occurs.
					- For general questions, provide a useful answer based on your existing knowledge.
					- For unsupported operations, answer what you can and clearly state any relevant
					  limitations.
					- Never claim an operation was executed or live data was retrieved unless it actually happened.

					# Output Rules
					- Return valid JSON only.
					- Do not include Markdown code fences or text outside the JSON object.
					- Use double quotes for JSON keys and string values.
					- Do not add fields beyond the specified schema.
					- Ensure the output can be parsed by a standard JSON parser.

					# Available Capabilities
					```json
					%s
					```

					# User Message
					%s
					"""
					.formatted(servicesJson, message);

		} catch (Exception e) {
			throw new IllegalStateException(
					"Failed to serialize available services", e);
		}
	}

	private Map<String, Object> initializeAvailableServices() {
		Map<String, Object> availableServices = new HashMap<>();

		availableServices.put("products", Map.of(
				"getAll", Map.of(
						"parameters", Map.of(
								"categoryIds",
								"List of category IDs to filter by, UUID",
								"brandId", "Brand ID to filter by, UUID",
								"vendorId", "Vendor ID to filter by, UUID"),
						"availableSort", List.of("name", "variants.price", "variants.stock", "createdAt")),
				"getById", Map.of(
						"parameters", Map.of(
								"id", "Product ID to retrieve, UUID"))));
		availableServices.put("orders", Map.of(
				"getAll", Map.of(
						"parameters", Map.of(
								"status", "Order status to filter by, String"),
						"availableSort", List.of("createdAt", "totalAmount", "status")),
				"getById", Map.of(
						"parameters", Map.of(
								"id", "Order ID to retrieve, UUID"))));
		availableServices.put("users", Map.of(
				"getAll", Map.of(
						"parameters", Map.of(),
						"availableSort", List.of("name", "price", "createdAt")),
				"getById", Map.of(
						"parameters", Map.of(
								"id", "User ID to retrieve, UUID"))));
		availableServices.put("brands", Map.of(
				"getAll", Map.of(
						"parameters", Map.of(),
						"availableSort", List.of("name", "email", "total")),
				"getById", Map.of(
						"parameters", Map.of(
								"id", "Brand ID to retrieve, UUID"))));
		availableServices.put("categories", Map.of(
				"getAll", Map.of(
						"parameters", Map.of(),
						"availableSort", List.of("name")),
				"getById", Map.of(
						"parameters", Map.of(
								"id", "Category ID to retrieve, UUID"))));
		availableServices.put("vendors", Map.of(
				"getAll", Map.of(
						"parameters", Map.of(),
						"availableSort", List.of("name")),
				"getById", Map.of(
						"parameters", Map.of(
								"id", "Vendor ID to retrieve, UUID"))));

		return availableServices;
	}
}
