package com.backbase.ct.dataloader.configurator;

import static com.backbase.ct.dataloader.data.ProductSummaryDataGenerator.generateBalanceHistoryPostRequestBodies;
import static org.apache.http.HttpStatus.SC_CREATED;

import com.backbase.ct.dataloader.client.productsummary.ArrangementsIntegrationRestClient;
import com.backbase.ct.dataloader.data.CommonConstants;
import com.backbase.ct.dataloader.data.ProductSummaryDataGenerator;
import com.backbase.ct.dataloader.dto.ArrangementId;
import com.backbase.ct.dataloader.util.CommonHelpers;
import com.backbase.ct.dataloader.util.GlobalProperties;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBody;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostRequestBodyParent;
import com.backbase.integration.arrangement.rest.spec.v2.arrangements.ArrangementsPostResponseBody;
import com.backbase.integration.arrangement.rest.spec.v2.balancehistory.BalanceHistoryPostRequestBody;
import com.backbase.integration.arrangement.rest.spec.v2.products.ProductsPostRequestBody;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductSummaryConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductSummaryConfigurator.class);
    private static GlobalProperties globalProperties = GlobalProperties.getInstance();
    private Random random = new Random();
    private final ArrangementsIntegrationRestClient arrangementsIntegrationRestClient;

    public void ingestProducts() throws IOException {
        ProductsPostRequestBody[] products = ProductSummaryDataGenerator.generateProductsPostRequestBodies();
        Arrays.stream(products).parallel()
            .forEach(product -> arrangementsIntegrationRestClient.ingestProductAndLogResponse(product));
    }

    public List<ArrangementId> ingestArrangementsByLegalEntity(String externalLegalEntityId,
        ArrangementsPostRequestBodyParent.Currency currency) {
        List<ArrangementId> arrangementIds = Collections.synchronizedList(new ArrayList<>());

        int randomAmount = CommonHelpers
            .generateRandomNumberInRange(globalProperties.getInt(CommonConstants.PROPERTY_ARRANGEMENTS_MIN),
                globalProperties.getInt(CommonConstants.PROPERTY_ARRANGEMENTS_MAX));
        IntStream.range(0, randomAmount).parallel().forEach(randomNumber -> {
            ArrangementsPostRequestBodyParent.Currency arrangementCurrency = currency == null
                ? ArrangementsPostRequestBodyParent.Currency
                .values()[random.nextInt(ArrangementsPostRequestBodyParent.Currency.values().length)]
                : currency;
            ArrangementsPostRequestBody arrangement = ProductSummaryDataGenerator
                .generateArrangementsPostRequestBody(externalLegalEntityId, arrangementCurrency);

            ArrangementsPostResponseBody arrangementsPostResponseBody = arrangementsIntegrationRestClient
                .ingestArrangement(arrangement)
                .then()
                .statusCode(SC_CREATED)
                .extract()
                .as(ArrangementsPostResponseBody.class);

            arrangementIds.add(new ArrangementId(arrangementsPostResponseBody.getId(), arrangement.getId()));

            LOGGER.info("Arrangement [{}] with currency [{}] ingested for product [{}] under legal entity [{}]",
                arrangement.getName(), arrangementCurrency, arrangement.getProductId(), externalLegalEntityId);
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

                LOGGER.info("Balance history item ingested for arrangement [{}] with updated date [{}]",
                    externalArrangementId, balanceHistoryPostRequestBody.getUpdatedDate());
            });
    }
}
