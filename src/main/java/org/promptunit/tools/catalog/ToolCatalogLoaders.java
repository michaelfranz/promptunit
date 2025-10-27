package org.promptunit.tools.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;

public final class ToolCatalogLoaders {
	private static final ServiceLoader<ToolCatalogLoader> LOADER = ServiceLoader.load(ToolCatalogLoader.class);

	private ToolCatalogLoaders() {}

	public static ToolCatalog load(Path path) throws IOException {
		String name = path.getFileName().toString().toLowerCase();
		try (InputStream in = Files.newInputStream(path)) {
			for (ToolCatalogLoader loader : LOADER) {
				if (loader.supports(name)) {
					return loader.load(in, path.toUri());
				}
			}
		}
		throw new IOException("No ToolCatalogLoader found for " + name);
	}
}
