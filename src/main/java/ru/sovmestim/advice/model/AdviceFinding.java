package ru.sovmestim.advice.model;

import java.util.List;

public record AdviceFinding(
        AdviceKind kind,
        AdviceLevel level,
        String title,
        String text,
        List<String> substances,
        List<AdviceSourceRef> sources) {}
