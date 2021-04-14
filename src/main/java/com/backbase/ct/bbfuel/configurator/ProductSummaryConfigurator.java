package com.backbase.ct.bbfuel.configurator;

import static com.backbase.ct.bbfuel.data.ProductSummaryDataGenerator.generateBalanceHistoryPostRequestBodies;
import static com.backbase.ct.bbfuel.data.ProductSummaryDataGenerator.generateCurrentAccountArrangementsPostRequestBodies;
import static com.backbase.ct.bbfuel.data.ProductSummaryDataGenerator.generateNonCurrentAccountArrangementsPostRequestBodies;
import static java.util.Collections.synchronizedList;
import static org.apache.http.HttpStatus.SC_CREATED;

import com.backbase.ct.bbfuel.client.productsummary.ArrangementsIntegrationRestClient;
import com.backbase.ct.bbfuel.data.ProductSummaryDataGenerator;
import com.backbase.ct.bbfuel.dto.ArrangementId;
import com.backbase.ct.bbfuel.dto.entitlement.ProductGroupSeed;
import com.backbase.dbs.arrangement.integration.inbound.api.v2.model.Subscription;
import com.backbase.dbs.arrangement.integration.rest.spec.v2.arrangements.ArrangementsPostResponseBody;
import com.backbase.dbs.arrangement.integration.rest.spec.v2.balancehistory.BalanceHistoryPostRequestBody;
import com.backbase.dbs.arrangement.integration.rest.spec.v2.products.ProductsPostRequestBody;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBody;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSummaryConfigurator {

    private final ArrangementsIntegrationRestClient arrangementsIntegrationRestClient;

    public void ingestProducts() {
        List<ProductsPostRequestBody> products = ProductSummaryDataGenerator.getProductsFromFile();
        products.stream().parallel()
            .forEach(arrangementsIntegrationRestClient::ingestProductAndLogResponse);
    }

    public List<ArrangementId> ingestArrangements(String externalLegalEntityId, ProductGroupSeed productGroupSeed) {
        List<ArrangementsPostRequestBody> arrangements = synchronizedList(new ArrayList<>());
        List<ArrangementId> arrangementIds = synchronizedList(new ArrayList<>());
        List<String> productIds = productGroupSeed.getProductIds();
        String positivePaySubscription = "checks-positive-pay-without-payee-match";

        int numberOfArrangements = productGroupSeed.getNumberOfArrangements().getRandomNumberInRange();
        int tenPercentOfTotal = (int) Math.round(numberOfArrangements * 0.1);
        int minNumberOfNonCurrentAccounts = (productIds.contains(String.valueOf(1)) && productIds.size() > 1)
            || (!productIds.contains(String.valueOf(1)) && !productIds.isEmpty()) ? 1 : 0;
        int numberOfNonCurrentAccounts = tenPercentOfTotal > 0 ? tenPercentOfTotal : minNumberOfNonCurrentAccounts;

        if (productGroupSeed.getProductIds().contains(String.valueOf(1))) {
            arrangements.addAll(generateCurrentAccountArrangementsPostRequestBodies(
                externalLegalEntityId, productGroupSeed, numberOfArrangements - numberOfNonCurrentAccounts));
            productGroupSeed.getProductIds().remove(String.valueOf(1));
        }

        if (numberOfNonCurrentAccounts > 0 && !productGroupSeed.getProductIds().isEmpty()) {
            arrangements.addAll(generateNonCurrentAccountArrangementsPostRequestBodies(
                externalLegalEntityId, productGroupSeed,
                productGroupSeed.getProductIds().contains(String.valueOf(1)) ? numberOfNonCurrentAccounts : numberOfArrangements));
        }

        arrangements.parallelStream().forEach(arrangement -> {
            ArrangementsPostResponseBody arrangementsPostResponseBody = arrangementsIntegrationRestClient
                .ingestArrangement(arrangement);
            arrangementsIntegrationRestClient.postSubscriptions(arrangement.getId(),
                    new Subscription().identifier(positivePaySubscription));
            log.info("Arrangement [{}] ingested for product [{}] under legal entity [{}]",
                arrangement.getName(), arrangement.getProductId(), externalLegalEntityId);
            arrangementIds.add(new ArrangementId(arrangementsPostResponseBody.getId(), arrangement.getId()));
        });

        return arrangementIds;
    }

    public void ingestBalanceHistory(String externalArrangementId) {
        List<BalanceHistoryPostRequestBody> balanceHistoryPostRequestBodies = generateBalanceHistoryPostRequestBodies(
            externalArrangementId);

        balanceHistoryPostRequestBodies.parallelStream()
            .forEach(balanceHistoryPostRequestBody -> {
                arrangementsIntegrationRestClient.ingestBalance(balanceHistoryPostRequestBody)
                    .then()
                    .statusCode(SC_CREATED);

                log.info("Balance history item ingested for arrangement [{}] with updated date [{}]",
                    externalArrangementId, balanceHistoryPostRequestBody.getUpdatedDate());
            });
    }
}
