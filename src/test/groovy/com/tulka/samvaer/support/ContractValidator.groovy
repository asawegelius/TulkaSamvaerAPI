package com.tulka.samvaer.support

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaValidatorsConfig
import com.networknt.schema.SpecVersion

/**
 * Checks responses against api/openapi.yaml (copied to src/test/resources/contract): the status
 * must be documented for the operation, and the body must match its schema, required fields,
 * types, enums and formats included.
 */
class ContractValidator {

    static final String BASE = 'https://samvaer.test/openapi.json'

    final JsonNode spec
    private final JsonSchemaFactory factory
    private final SchemaValidatorsConfig config
    private final ObjectMapper json = new ObjectMapper()

    ContractValidator() {
        def yaml = new ObjectMapper(new YAMLFactory())
        spec = yaml.readTree(getClass().getResourceAsStream('/contract/openapi.yaml'))
        String specJson = json.writeValueAsString(spec)
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012) { builder ->
            builder.schemaLoaders { loaders -> loaders.schemas([(BASE): specJson]) }
        }
        config = SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build()
    }

    /** Path templates and methods in the contract, as "GET /api/v1/events/{eventId}". */
    Set<String> operations() {
        def result = [] as Set<String>
        spec.get('paths').fields().each { path ->
            path.value.fieldNames().findAll { it in ['get', 'post', 'put', 'delete', 'patch'] }.each {
                result << "${it.toUpperCase()} ${path.key}".toString()
            }
        }
        result
    }

    /** Fails with a readable message when the response does not match the contract. */
    void check(String method, String path, int status, String contentType, String body) {
        String template = template(path)
        assert template != null: "No path in openapi.yaml matches $path"
        JsonNode operation = spec.get('paths').get(template).get(method.toLowerCase())
        assert operation != null: "openapi.yaml has no $method $template"
        String pointer = "/paths/${escape(template)}/${method.toLowerCase()}/responses/$status"
        JsonNode response = operation.get('responses').get(String.valueOf(status))
        assert response != null: "$method $template answered $status, which openapi.yaml does not list"
        if (response.has('$ref')) {
            pointer = response.get('$ref').asText().substring(1)
            response = spec.at(pointer)
        }
        JsonNode content = response.get('content')
        if (content == null) {
            assert !body: "$method $template $status should have no body, got: $body"
            return
        }
        String mediaType = content.fieldNames().next()
        assert contentType?.startsWith(mediaType): "$method $template $status: expected $mediaType, got $contentType"
        String schemaPointer = "$pointer/content/${escape(mediaType)}/schema"
        def schema = factory.getSchema(SchemaLocation.of("$BASE#$schemaPointer"), config)
        def errors = schema.validate(json.readTree(body))
        assert errors.isEmpty(): "$method $template $status does not match openapi.yaml:\n  " +
                errors*.message.join('\n  ') + "\nBody: $body"
    }

    private String template(String path) {
        String bare = path.contains('?') ? path.substring(0, path.indexOf('?')) : path
        spec.get('paths').fieldNames().find { t ->
            bare ==~ ('^' + t.replaceAll(/\{[^}]+}/, '[^/]+') + '$')
        }
    }

    private static String escape(String token) {
        token.replace('~', '~0').replace('/', '~1')
    }
}
