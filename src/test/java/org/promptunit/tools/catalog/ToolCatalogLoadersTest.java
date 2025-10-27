package org.promptunit.tools.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class ToolCatalogLoadersTest {
	@Test
	void loadOpenApiYaml() throws Exception {
		Path p = Path.of("src/test/resources/tools-openapi.yaml");
		ToolCatalog c = ToolCatalogLoaders.load(p);
		assertThat(c.getTools()).isNotEmpty();
		assertThat(c.getTools()).anyMatch(t -> t.getName().equals("CreateIssue"));
	}

	@Test
	void loadOpenAiToolsJson() throws Exception {
		Path p = Path.of("src/test/resources/tools-openai.json");
		ToolCatalog c = ToolCatalogLoaders.load(p);
		assertThat(c.getTools()).isNotEmpty();
		assertThat(c.getTools()).anyMatch(t -> t.getName().equals("DeleteIssue"));
	}

	@Test
	void loadSystemToolsJson() throws Exception {
		Path p = Path.of("src/test/resources/tools-system.json");
		ToolCatalog c = ToolCatalogLoaders.load(p);
		assertThat(c.getTools()).isNotEmpty();
		assertThat(c.getTools()).anyMatch(t -> t.getName().equals("AskForClarification"));
	}
}
