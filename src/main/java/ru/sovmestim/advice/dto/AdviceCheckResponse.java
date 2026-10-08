package ru.sovmestim.advice.dto;

import java.util.UUID;
import ru.sovmestim.advice.model.AdviceResult;

public record AdviceCheckResponse(UUID adviceId, AdviceResult result) {}
