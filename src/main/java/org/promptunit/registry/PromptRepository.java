package org.promptunit.registry;

import java.util.List;

public interface PromptRepository {
    List<PromptMetadata> findAll();
}


