package org.promptunit.registry;

import java.util.Optional;

public interface PromptLibrary {
    Optional<PromptMetadata> findById(String id);
}


