package com.wire.bots.hold;

import com.wire.bots.hold.DAO.MetadataDAO;
import com.wire.bots.hold.model.Metadata;
import com.wire.bots.hold.utils.Cache;
import com.wire.helium.LoginClient;
import com.wire.helium.models.BackendConfiguration;
import com.wire.xenon.exceptions.HttpException;
import com.wire.xenon.tools.Logger;

public class FallbackDomainFetcher implements Runnable {

    private final LoginClient loginClient;
    private final MetadataDAO metadataDAO;

    /**
     * Fetcher and handler for fallback domain.
     * <p>
     *     Fetches from API and compares against database value (if any), then inserts into database and updates cache value.
     *     If value received from the API is different from what is saved in the database, a [RuntimeException] is thrown.
     * </p>
     * @param loginClient [{@link LoginClient}] as API to get backend configuration containing default domain.
     * @param metadataDAO [{@link MetadataDAO}] as DAO to get/insert default domain to database.
     *
     * @throws RuntimeException if received domain from API is different from the one saved in the database.
     */
    FallbackDomainFetcher(LoginClient loginClient, MetadataDAO metadataDAO) {
        this.loginClient = loginClient;
        this.metadataDAO = metadataDAO;
    }

    @Override
    public void run() {
        if (Cache.getFallbackDomain() != null) { return; }

        Metadata metadata = metadataDAO.get(MetadataDAO.FALLBACK_DOMAIN_KEY);
        try {
            BackendConfiguration apiVersionResponse = loginClient.getBackendConfiguration();

            if (metadata == null) {
                metadataDAO.insert(MetadataDAO.FALLBACK_DOMAIN_KEY, apiVersionResponse.domain);
                Cache.setFallbackDomain(apiVersionResponse.domain);
            } else {
                if (metadata.value.equals(apiVersionResponse.domain)) {
                    Cache.setFallbackDomain(apiVersionResponse.domain);
                } else {
                    String formattedExceptionMessage = String.format(
                        "Database already has a default domain as %s and instead we got %s from the Backend API.",
                        metadata.value,
                        apiVersionResponse.domain
                    );
                    throw new RuntimeException(formattedExceptionMessage);
                }
            }
        } catch (HttpException exception) {
            Logger.exception(exception, "FallbackDomainFetcher.run, exception: %s", exception.getMessage());
        }
    }
}
