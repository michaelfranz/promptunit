package org.promptunit.tools.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public interface ToolCatalogLoader {
	boolean supports(String contentTypeOrFilename);
	ToolCatalog load(InputStream in, URI source) throws IOException;
}
