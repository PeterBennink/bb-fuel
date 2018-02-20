package com.backbase.testing.dataloader.configurators;

import com.backbase.presentation.contact.rest.spec.v2.contacts.ContactsPostRequestBody;
import com.backbase.testing.dataloader.clients.contact.ContactPresentationRestClient;
import com.backbase.testing.dataloader.data.ContactsDataGenerator;
import com.backbase.testing.dataloader.utils.CommonHelpers;
import com.backbase.testing.dataloader.utils.GlobalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_CONTACTS_MAX;
import static com.backbase.testing.dataloader.data.CommonConstants.PROPERTY_CONTACTS_MIN;
import static com.backbase.testing.dataloader.data.ContactsDataGenerator.generateContactsPostRequestBody;
import static org.apache.http.HttpStatus.SC_CREATED;

public class ContactsConfigurator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactsConfigurator.class);
    private static GlobalProperties globalProperties = GlobalProperties.getInstance();

    private ContactPresentationRestClient contactPresentationRestClient = new ContactPresentationRestClient();

    public void ingestContacts() {
        int randomAmount = CommonHelpers.generateRandomNumberInRange(globalProperties.getInt(PROPERTY_CONTACTS_MIN), globalProperties.getInt(PROPERTY_CONTACTS_MAX));
        IntStream.range(0, randomAmount).parallel().forEach(randomNumber -> {
            ContactsPostRequestBody contact = generateContactsPostRequestBody();
            contactPresentationRestClient.createContact(contact)
                    .then()
                    .statusCode(SC_CREATED);

            LOGGER.info(String.format("Contact ingested with name [%s]", contact.getName()));
        });
    }
}