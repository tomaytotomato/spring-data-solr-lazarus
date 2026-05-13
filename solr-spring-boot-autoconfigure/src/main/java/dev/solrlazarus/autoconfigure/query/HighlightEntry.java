package dev.solrlazarus.autoconfigure.query;

import java.util.List;
import java.util.Map;

public record HighlightEntry<T>(T entity, Map<String, List<String>> highlights) {
}
