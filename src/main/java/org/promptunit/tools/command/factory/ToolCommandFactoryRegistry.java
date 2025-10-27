package org.promptunit.tools.command.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class ToolCommandFactoryRegistry {
    private final List<ToolCommandFactory> factories;

    private ToolCommandFactoryRegistry(List<ToolCommandFactory> factories) {
        this.factories = List.copyOf(factories);
    }

    public static ToolCommandFactoryRegistry of(List<ToolCommandFactory> factories) {
        return new ToolCommandFactoryRegistry(new ArrayList<>(factories));
    }

    public static ToolCommandFactoryRegistry usingServiceLoader() {
        List<ToolCommandFactory> loaded = new ArrayList<>();
        ServiceLoader.load(ToolCommandFactory.class).forEach(loaded::add);
        return new ToolCommandFactoryRegistry(loaded);
    }

    public Optional<ToolCommandFactory> findFactory(String provider, String model, String toolId, Optional<String> toolVersion) {
        for (ToolCommandFactory f : factories) {
            if (f.supports(provider, model, toolId, toolVersion)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }
}


