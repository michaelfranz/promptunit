package org.promptunit.env;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DotenvEnvironmentUtil {
    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentUtil.class);

	private static Map<String, Object> envVarValues = new HashMap<>();

    public static String getEnvVarStringValue(String key) {
        Object value = getEnvVarValue(key);
        return value != null ? value.toString() : null;
    }

    public static Object getEnvVarValue(String key) {
        return envVarValues.computeIfAbsent(key, DotenvEnvironmentUtil::computeKeyValue);
    }

    private static Object computeKeyValue(String key) {
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }
        // Fallback: look up in .env files located in CWD or subfolders (as per locateEnvFiles)
        try {
            for (File envFile : locateEnvFiles()) {
                try (BufferedReader br = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Map.Entry<String, Object> entry = getStringObjectEntry(line);
                        if (entry == null) continue;
                        if (key.equals(entry.getKey())) {
                            return entry.getValue();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed reading .env files while resolving {}: {}", key, e.toString());
        }
        return null;
    }

    private static Map<String, Object> getEnvVarValues() throws IOException {
        return getEnvVarValues(locateEnvFiles());
    }

    private static Map<String, Object> getEnvVarValues(List<File> envFiles) throws IOException {
        var values = new LinkedHashMap<String, Object>();
        for (File envFile : envFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    Map.Entry<String, Object> entry = getStringObjectEntry(line);
                    if (entry == null) continue;
                    values.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return values;
    }

	private static Map.Entry<String, Object> getStringObjectEntry(String line) {
		line = line.trim();
		if (line.isEmpty() || line.startsWith("#")) return null;
		int eq = line.indexOf('=');
		if (eq <= 0) return null;
		String key = line.substring(0, eq).trim();
		String val = line.substring(eq + 1).trim();
		if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
			val = val.substring(1, val.length() - 1);
		}
		// Do not override real env or system properties
		if (System.getProperty(key) != null || System.getenv(key) != null) {
			return null;
		}
		return Map.entry(key, val);
	}

    private static void readDotEnvFiles() {
		log.info(">>> DotenvEnvironmentUtil.postProcessEnvironment() called");
		try {
			var envFiles = locateEnvFiles();
			if (envFiles.isEmpty()) {
				log.info(">>> No .env files found");
				return;
			}
            var values = getEnvVarValues(envFiles);
			if (!values.isEmpty()) {
				for (File envFile: envFiles) {
					log.info("Loaded {} entries from .env at {}", values.size(), envFile.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			log.warn("Failed to load .env: {}", e.toString());
		}
	}

    private static List<File> locateEnvFiles() {
        List<File> found = new ArrayList<>();
        try {
            Path start = new File(".").toPath();
            Files.walk(start, 2)
                    .filter(p -> ".env".equals(p.getFileName().toString()))
                    .filter(Files::isRegularFile)
                    .forEach(p -> found.add(p.toFile()));
        } catch (IOException e) {
            log.warn("Failed to search for .env files: {}", e.toString());
        }
        return found;
    }

}
