package com.wire.bots.hold.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CacheTest {

    @Before
    public void before() {
        // Clears cached domain
        Cache.setFallbackDomain(null);
    }

    @After
    public void after() {
        // Clears cached domain
        Cache.setFallbackDomain(null);
    }

    @Test
    public void verifyDefaultDomainIsSetCorrectly() {
        String firstDomain = Cache.getFallbackDomain();
        assert firstDomain == null;

        Cache.setFallbackDomain("dummy_domain");
        String secondDomain = Cache.getFallbackDomain();

        assert secondDomain != null;
        assert secondDomain.equals("dummy_domain");

        Cache.setFallbackDomain("dummy_domain_3");
        String thirdDomain = Cache.getFallbackDomain();

        assert thirdDomain != null;
        assert thirdDomain.equals("dummy_domain_3");
    }
}
