package com.backbase.ct.bbfuel.input;

import static com.backbase.ct.bbfuel.util.CommonHelpers.getRandomFromList;
import static java.util.Arrays.asList;

import com.backbase.ct.bbfuel.data.CommonConstants;
import com.backbase.ct.bbfuel.util.CommonHelpers;
import com.backbase.ct.bbfuel.util.ParserUtil;
import com.backbase.dbs.transaction.client.v2.model.Currency;
import com.backbase.dbs.transaction.client.v2.model.TransactionsPostRequestBody;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionsReader extends BaseReader {

    public TransactionsPostRequestBody loadSingle(String externalArrangementId) {
        return getRandomFromList(load(globalProperties.getString(CommonConstants.PROPERTY_TRANSACTIONS_DATA_JSON)))
                .id(UUID.randomUUID().toString())
                .arrangementId(externalArrangementId)
                .bookingDate(LocalDate.now());
    }

    /**
     * Load transactions with reference filled with pocket external arrangement id.
     *
     * @param externalArrangementId parent pocket external arrangement id
     * @param pocketArrangementId   pocket arrangement id
     * @return list of TransactionsPostRequestBody's
     */
    public List<TransactionsPostRequestBody> loadWithPocketAsReference(String externalArrangementId,
        String pocketArrangementId) {
        List<TransactionsPostRequestBody> list = load(
            globalProperties.getString(CommonConstants.PROPERTY_POCKET_TRANSACTIONS_DATA_JSON));
        list.forEach(transactionsPostRequestBody -> {
            transactionsPostRequestBody
                .id(UUID.randomUUID().toString())
                .arrangementId(externalArrangementId)
                .reference(pocketArrangementId)
                .bookingDate(LocalDate.now());
        });
        return list;
    }

    /**
     * Load transactions with reference filled with pocket parent external arrangement id.
     *
     * @param currentAccountExternalArrangementId current account external arrangement id
     * @param parentPocketExternalArrangementId   parent pocket external arrangement id
     * @return list of TransactionsPostRequestBody's
     */
    public List<TransactionsPostRequestBody> loadWithPocketParentAsReference(String currentAccountExternalArrangementId,
        String parentPocketExternalArrangementId) {
        List<TransactionsPostRequestBody> list = load(
            globalProperties.getString(CommonConstants.PROPERTY_CURRENTACCOUNT_TRANSACTIONS_DATA_JSON));
        list.forEach(transactionsPostRequestBody -> {
            transactionsPostRequestBody
                .id(UUID.randomUUID().toString())
                .arrangementId(currentAccountExternalArrangementId)
                .reference(parentPocketExternalArrangementId)
                .bookingDate(LocalDate.now());
        });
        return list;
    }

    /**
     * To be able to find check images in front end apps easily, created transactions should match with
     * transaction-integration-check-images-api
     * And the booking date and value date should be set to today so that we can easily find that transaction and test it.
     */
    public TransactionsPostRequestBody loadSingleWithCheckImages(String externalArrangementId) {
        return getRandomFromList(load(globalProperties.getString(CommonConstants.PROPERTY_TRANSACTIONS_CHECK_IMAGES_DATA_JSON)))
                .arrangementId(externalArrangementId)
                .bookingDate(LocalDate.now())
                .valueDate(LocalDate.now());
    }

    private List<TransactionsPostRequestBody> load(String uri) {
        List<TransactionsPostRequestBody> transactions;

        try {
            TransactionsPostRequestBody[] parsedTransactions = ParserUtil.convertJsonToObject(uri, TransactionsPostRequestBody[].class);
            transactions = asList(parsedTransactions);
        } catch(IOException e) {
            log.error("Failed parsing file with Transactions", e);
            throw new InvalidInputException(e.getMessage(), e);
        }
        return transactions;
    }
}
