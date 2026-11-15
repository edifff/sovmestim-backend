package ru.sovmestim.advice.client;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import ru.sovmestim.advice.model.PatientAllergy;
import ru.sovmestim.advice.model.PatientCondition;
import ru.sovmestim.common.grpc.GrpcClientFactory;
import ru.sovmestim.proto.patient.v1.GetAllergiesRequest;
import ru.sovmestim.proto.patient.v1.GetConditionsRequest;
import ru.sovmestim.proto.patient.v1.PatientServiceGrpc;

/**
 * gRPC patient-profile client with an explicit deadline on every call.
 */
@Component
@RequiredArgsConstructor
public class GrpcPatientClient implements PatientClient {

    private static final long DEADLINE_SECONDS = 2;

    private final GrpcClientFactory channels;

    @Override
    public List<PatientAllergy> allergies(UUID userId) {
        return stub()
                .getAllergies(GetAllergiesRequest.newBuilder().setUserId(userId.toString()).build())
                .getAllergiesList()
                .stream()
                .map(allergy -> new PatientAllergy(
                        allergy.getName(),
                        ContractMapper.textOrNull(allergy.getCode()),
                        ContractMapper.textOrNull(allergy.getSeverity())))
                .toList();
    }

    @Override
    public List<PatientCondition> conditions(UUID userId) {
        return stub()
                .getConditions(GetConditionsRequest.newBuilder().setUserId(userId.toString()).build())
                .getConditionsList()
                .stream()
                .map(condition -> new PatientCondition(
                        condition.getName(), ContractMapper.textOrNull(condition.getMkbCode())))
                .toList();
    }

    private PatientServiceGrpc.PatientServiceBlockingStub stub() {
        return PatientServiceGrpc.newBlockingStub(channels.channel("patient"))
                .withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
