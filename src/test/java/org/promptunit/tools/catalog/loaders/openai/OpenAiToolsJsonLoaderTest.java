package org.promptunit.tools.catalog.loaders.openai;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.promptunit.tools.catalog.ToolCatalog;
import org.promptunit.tools.catalog.ToolSpec;

class OpenAiToolsJsonLoaderTest {
	private final OpenAiToolsJsonLoader loader = new OpenAiToolsJsonLoader();

	@Test
	void supports_json_files() {
		assertThat(loader.supports("tools.json")).isTrue();
		assertThat(loader.supports("TOOLS.JSON")).isTrue();
		assertThat(loader.supports("tools.yaml")).isFalse();
	}

	@Test
	void loads_domain_and_system_tools() throws Exception {
		Path path = Path.of("src/test/resources/tools-openai.json");
		try (InputStream in = Files.newInputStream(path)) {
			ToolCatalog catalog = loader.load(in, path.toUri());
			List<ToolSpec> tools = catalog.getTools();
			assertThat(tools).isNotEmpty();

			assertThat(tools).anyMatch(t -> t.getName().equals("GetIssue"));
			assertThat(tools).anyMatch(t -> t.getName().equals("CreateIssue"));
			assertThat(tools).anyMatch(t -> t.getName().equals("DeleteIssue"));
			assertThat(tools).anyMatch(t -> t.getName().equals("AskForClarification"));
			assertThat(tools).anyMatch(t -> t.getName().equals("RefuseWithReason"));

			ToolSpec delete = tools.stream().filter(t -> t.getName().equals("DeleteIssue")).findFirst().orElseThrow();
			assertThat(delete.getParametersSchema()).isNotNull();
			assertThat(delete.getParametersSchema().get("required")).isNotNull();
			assertThat(delete.getRisk()).isNotNull();
		}
	}

	@Test
	void loads_system_only_catalog() throws Exception {
		Path path = Path.of("src/test/resources/tools-system.json");
		try (InputStream in = Files.newInputStream(path)) {
			ToolCatalog catalog = loader.load(in, path.toUri());
			List<ToolSpec> tools = catalog.getTools();
			assertThat(tools).isNotEmpty();
			assertThat(tools).anyMatch(t -> t.getName().equals("ShowMessage"));
			assertThat(tools).anyMatch(t -> t.getName().equals("AskForClarification"));
			assertThat(tools).anyMatch(t -> t.getName().equals("RefuseWithReason"));
		}
	}
}
