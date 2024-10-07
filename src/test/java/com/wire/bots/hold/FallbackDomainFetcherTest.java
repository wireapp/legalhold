package com.wire.bots.hold;

import com.wire.bots.hold.DAO.MetadataDAO;
import com.wire.bots.hold.model.Metadata;
import com.wire.bots.hold.utils.Cache;
import com.wire.helium.LoginClient;
import com.wire.helium.models.BackendConfiguration;
import com.wire.xenon.exceptions.HttpException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class FallbackDomainFetcherTest {

    private MetadataDAO metadataDAO;
    private LoginClient loginClient;
    private FallbackDomainFetcher fallbackDomainFetcher;

    @Before
    public void before() {
        // Clears cached domain
        Cache.setFallbackDomain(null);
        metadataDAO = mock(MetadataDAO.class);
        loginClient = mock(LoginClient.class);
        fallbackDomainFetcher = new FallbackDomainFetcher(loginClient, metadataDAO);
    }

    @After
    public void after() {
        // Clears cached domain
        Cache.setFallbackDomain(null);
    }

    @Test
    public void givenNoFallbackDomainInCacheAndDatabase_whenExecuting_thenFetchFromApiAndStoreInCacheAndDatabase() throws HttpException {
        // given
        BackendConfiguration backendConfiguration = new BackendConfiguration();
        backendConfiguration.domain = "dummy_domain_3";

        when(metadataDAO.get(MetadataDAO.FALLBACK_DOMAIN_KEY)).thenReturn(null);
        when(metadataDAO.insert(any(), any())).thenReturn(1);
        when(loginClient.getBackendConfiguration()).thenReturn(backendConfiguration);

        // when
        fallbackDomainFetcher.run();

        // then
        assert Cache.getFallbackDomain().equals("dummy_domain_3");
        verify(metadataDAO, times(1)).insert(MetadataDAO.FALLBACK_DOMAIN_KEY, backendConfiguration.domain);
    }

    @Test
    public void givenNoFallbackDomainInCache_whenExecuting_thenFetchFromAPIAndCompareWithDatabase() throws HttpException {
        // given
        BackendConfiguration backendConfiguration = new BackendConfiguration();
        backendConfiguration.domain = "dummy_domain_2";

        Metadata metadata = new Metadata(MetadataDAO.FALLBACK_DOMAIN_KEY, "dummy_domain_2");

        when(metadataDAO.get(MetadataDAO.FALLBACK_DOMAIN_KEY)).thenReturn(metadata);
        when(loginClient.getBackendConfiguration()).thenReturn(backendConfiguration);

        // when
        fallbackDomainFetcher.run();

        // then
        assert Cache.getFallbackDomain().equals("dummy_domain_2");
    }

    @Test(expected=RuntimeException.class)
    public void givenNoFallbackDomainInCache_whenExecutingAndApiReturnsDifferentDomainFromDatabase_thenThrowRuntimeException() throws HttpException, RuntimeException {
        // given
        BackendConfiguration backendConfiguration = new BackendConfiguration();
        backendConfiguration.domain = "dummy_domain_1";

        Metadata metadata = new Metadata(MetadataDAO.FALLBACK_DOMAIN_KEY, "dummy_domain_2");

        when(metadataDAO.get(MetadataDAO.FALLBACK_DOMAIN_KEY)).thenReturn(metadata);
        when(loginClient.getBackendConfiguration()).thenReturn(backendConfiguration);

        // when
        fallbackDomainFetcher.run();
    }

    @Test
    public void givenFallbackDomainInCache_whenExecuting_thenReturnAndIgnoreDatabaseAndApiCalls() {
        // given
        Cache.setFallbackDomain("dummy_domain_0");

        // when
        fallbackDomainFetcher.run();

        // then
        assert Cache.getFallbackDomain().equals("dummy_domain_0");
    }
}
