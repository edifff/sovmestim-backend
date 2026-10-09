package ru.sovmestim.advice.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import ru.sovmestim.advice.model.AdviceLevel;

/**
 * Loads {@code rls_class_mapping.yaml}.
 */
public final class ClassMappingLoader {

    private ClassMappingLoader() {
    }

    /**
     * Parses the class-mapping file into an immutable mapping table.
     *
     * @param resource YAML resource to read
     * @return parsed class-mapping table
     */
    @SuppressWarnings("unchecked")
    public static RlsClassMapping load(Resource resource) {
        Map<String, Object> root = readYaml(resource);
        String version = str(root.get("version"));
        AdviceLevel defaultLevel = level(root.get("default_level"), AdviceLevel.INFO);

        List<Map<String, Object>> mappings = (List<Map<String, Object>>) root.getOrDefault("mappings", List.of());
        List<RlsClassMapping.Entry> entries = mappings.stream()
                .map(ClassMappingLoader::toEntry)
                .toList();
        return new RlsClassMapping(version, defaultLevel, entries);
    }

    private static RlsClassMapping.Entry toEntry(Map<String, Object> map) {
        AdviceLevel level = level(map.get("level"), AdviceLevel.INFO);
        return new RlsClassMapping.Entry(
                str(map.get("class")),
                str(map.get("subclass")),
                str(map.get("direction")),
                level,
                str(map.get("explanation")));
    }

    private static AdviceLevel level(Object value, AdviceLevel fallback) {
        String text = str(value);
        if (text == null) {
            return fallback;
        }
        try {
            return AdviceLevel.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    static Map<String, Object> readYaml(Resource resource) {
        try (InputStream input = resource.getInputStream()) {
            return new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read rule file " + resource, ex);
        }
    }

    static String str(Object value) {
        return value == null ? null : value.toString();
    }
}
