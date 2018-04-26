package com.backbase.ct.dataloader.data;

import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.serviceagreements.CreateStatus;
import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.serviceagreements.Participant;
import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.serviceagreements.ServiceAgreementPostRequestBody;
import com.backbase.integration.accessgroup.rest.spec.v2.accessgroups.serviceagreements.ServiceAgreementPutRequestBody;
import com.github.javafaker.Faker;

import java.util.Set;
import java.util.UUID;

public class ServiceAgreementsDataGenerator {

    private static Faker faker = new Faker();

    public static ServiceAgreementPostRequestBody generateServiceAgreementPostRequestBody(Set<Participant> participants) {
        return new ServiceAgreementPostRequestBody()
            .withName(faker.lorem().sentence(3, 0).replace(".", ""))
            .withDescription(faker.lorem().sentence(3, 0).replace(".", ""))
            .withExternalId(UUID.randomUUID().toString())
            .withStatus(CreateStatus.ENABLED)
            .withParticipants(participants);
    }

    public static ServiceAgreementPutRequestBody generateServiceAgreementPutRequestBody() {
        return new ServiceAgreementPutRequestBody()
            .withExternalId(UUID.randomUUID()
                .toString());
    }
}