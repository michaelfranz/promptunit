package org.promptunit.tools.catalog.loaders.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.promptunit.tools.catalog.ToolCatalog;
import org.promptunit.tools.catalog.ToolSpec;

class OpenApiToolCatalogLoaderTest {
	private final OpenApiToolCatalogLoader loader = new OpenApiToolCatalogLoader();

	@Test
	void supports_yaml_json() {
		assertThat(loader.supports("a.yaml")).isTrue();
		assertThat(loader.supports("a.yml")).isTrue();
		assertThat(loader.supports("a.json")).isTrue();
	}

	@Test
	void loads_operations_and_parameters() throws Exception {
		Path path = Path.of("src/test/resources/tools-openapi.yaml");
		try (InputStream in = Files.newInputStream(path)) {
			ToolCatalog catalog = loader.load(in, path.toUri());
			List<ToolSpec> tools = catalog.getTools();
			assertThat(tools).isNotEmpty();
			assertThat(tools).anyMatch(t -> t.getName().equals("GetIssue"));
			assertThat(tools).anyMatch(t -> t.getName().equals("CreateIssue"));
			assertThat(tools).anyMatch(t -> t.getName().equals("AddLabel"));
			assertThat(tools).anyMatch(t -> t.getName().equals("DeleteIssue"));

			ToolSpec create = tools.stream().filter(t -> t.getName().equals("CreateIssue")).findFirst().orElseThrow();
			assertThat(create.getParametersSchema()).isNotNull();
			assertThat(create.getParametersSchema().get("type").asText()).isEqualTo("object");

			ToolSpec get = tools.stream().filter(t -> t.getName().equals("GetIssue")).findFirst().orElseThrow();
			assertThat(get.getParametersSchema()).isNotNull();
			assertThat(get.getParametersSchema().get("properties").has("id")).isTrue();
		}
	}
}
