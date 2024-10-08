// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.hold;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.hold.DAO.AccessDAO;
import com.wire.bots.hold.DAO.EventsDAO;
import com.wire.bots.hold.DAO.MetadataDAO;
import com.wire.bots.hold.filters.ServiceAuthenticationFilter;
import com.wire.bots.hold.healthchecks.SanityCheck;
import com.wire.bots.hold.monitoring.RequestMdcFactoryFilter;
import com.wire.bots.hold.monitoring.StatusResource;
import com.wire.bots.hold.resource.v0.audit.*;
import com.wire.bots.hold.resource.v0.backend.ConfirmResourceV0;
import com.wire.bots.hold.resource.v0.backend.InitiateResourceV0;
import com.wire.bots.hold.resource.v0.backend.RemoveResourceV0;
import com.wire.bots.hold.resource.v1.backend.ConfirmResourceV1;
import com.wire.bots.hold.resource.v1.backend.InitiateResourceV1;
import com.wire.bots.hold.resource.v1.backend.RemoveResourceV1;
import com.wire.bots.hold.service.DeviceManagementService;
import com.wire.bots.hold.utils.CryptoDatabaseFactory;
import com.wire.bots.hold.utils.HoldClientRepo;
import com.wire.bots.hold.utils.ImagesBundle;
import com.wire.helium.LoginClient;
import com.wire.xenon.Const;
import com.wire.xenon.backend.models.QualifiedId;
import com.wire.xenon.crypto.Crypto;
import com.wire.xenon.crypto.CryptoDatabase;
import com.wire.xenon.crypto.storage.JdbiStorage;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jdbi3.bundles.JdbiExceptionsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.flywaydb.core.Flyway;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.ws.rs.client.Client;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Service extends Application<Config> {
    public static Service instance;
    public static MetricRegistry metrics;
    protected Config config;
    protected Environment environment;
    protected Jdbi jdbi;

    public static void main(String[] args) throws Exception {
        instance = new Service();
        instance.run(args);
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));

        bootstrap.addBundle(new SwaggerBundle<>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(Config configuration) {
                return configuration.swagger;
            }
        });

        bootstrap.addBundle(new AssetsBundle("/assets/"));
        bootstrap.addBundle(new ImagesBundle("/opt/hold/images", "/images", "images"));
        bootstrap.addBundle(new ImagesBundle("/opt/hold/avatars", "/avatars", "avatars"));

        bootstrap.addBundle(new JdbiExceptionsBundle());

        Application<Config> application = bootstrap.getApplication();
        instance = (Service) application;
    }

    @Override
    public void run(Config config, Environment environment) throws ExecutionException, InterruptedException {
        this.config = config;
        this.environment = environment;
        Service.metrics = environment.metrics();

        System.setProperty(Const.WIRE_BOTS_SDK_TOKEN, config.token);
        System.setProperty(Const.WIRE_BOTS_SDK_API, config.apiHost);

        setupDatabase(config.database);

        final Client httpClient = createHttpClient(config, environment);
        jdbi = buildJdbi(config.database, environment);

        final CryptoDatabaseFactory cf = getCryptoFactory(jdbi);

        final AccessDAO accessDAO = jdbi.onDemand(AccessDAO.class);
        final EventsDAO eventsDAO = jdbi.onDemand(EventsDAO.class);
        final MetadataDAO metadataDAO = jdbi.onDemand(MetadataDAO.class);

        final DeviceManagementService deviceManagementService = new DeviceManagementService(accessDAO, cf);

        // Monitoring resources
        addResource(new StatusResource());
        addResource(new RequestMdcFactoryFilter());

        // Used by Wire Server
        addResource(new InitiateResourceV0(deviceManagementService));
        addResource(new InitiateResourceV1(deviceManagementService));
        addResource(new ConfirmResourceV0(deviceManagementService));
        addResource(new ConfirmResourceV1(deviceManagementService));
        addResource(new RemoveResourceV0(deviceManagementService));
        addResource(new RemoveResourceV1(deviceManagementService));

        // Used by Audit
        addResource(new AuthorizeResource());
        addResource(new DevicesResource(accessDAO, cf));
        addResource(new EventsResource(eventsDAO));
        addResource(new ConversationResource(jdbi, httpClient));
        addResource(new IndexResource(eventsDAO));

        addResource(ServiceAuthenticationFilter.ServiceAuthenticationFeature.class);

        final Future<?> fallbackDomainFetcher = environment
            .lifecycle()
            .executorService("fallback_domain_fetcher")
            .build()
            .submit(
                new FallbackDomainFetcher(
                    new LoginClient(httpClient),
                    metadataDAO
                )
            );

        fallbackDomainFetcher.get();

        environment.healthChecks().register(
            "SanityCheck",
            new SanityCheck(accessDAO, httpClient)
        );

        final HoldClientRepo repo = new HoldClientRepo(jdbi, cf, httpClient);

        final HoldMessageResource holdMessageResource = new HoldMessageResource(new MessageHandler(jdbi), repo);
        final NotificationProcessor notificationProcessor = new NotificationProcessor(httpClient, accessDAO, holdMessageResource);

        environment.lifecycle()
                .scheduledExecutorService("notifications")
                .build()
                .scheduleWithFixedDelay(notificationProcessor, 10, config.sleep.toSeconds(), TimeUnit.SECONDS);

        CollectorRegistry.defaultRegistry.register(new DropwizardExports(metrics));

        environment.getApplicationContext().addServlet(MetricsServlet.class, "/metrics");
    }

    public Config getConfig() {
        return config;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    protected void addResource(Object component) {
        environment.jersey().register(component);
    }

    private Client createHttpClient(Config config, Environment env) {
        return new JerseyClientBuilder(env)
                .using(config.getJerseyClient())
                .withProvider(MultiPartFeature.class)
                .withProvider(JacksonJsonProvider.class)
                .build(getName());
    }

    protected Jdbi buildJdbi(Config.Database database, Environment env) {
        return Jdbi
                .create(database.build(env.metrics(), getName()))
                .installPlugin(new SqlObjectPlugin());
    }

    protected void setupDatabase(Config.Database database) {
        Flyway flyway = Flyway
                .configure()
                .dataSource(database.getUrl(), database.getUser(), database.getPassword())
                .baselineOnMigrate(database.baseline)
                .load();
        flyway.migrate();
    }

    public CryptoDatabaseFactory getCryptoFactory(Jdbi jdbi) {
        return new CryptoDatabaseFactory() {
            @Override
            public Crypto create(UUID botId) throws CryptoException {
                // Note: the name botId is incorrect as in LegalHold we are creating crypto box based on users
                //  but in the Xenon library used by bots and LegalHold, this param was called botId
                return new CryptoDatabase(
                    new QualifiedId(botId, null),
                    new JdbiStorage(jdbi)
                );
            }

            @Override
            public Crypto create(QualifiedId userId) throws CryptoException {
                return new CryptoDatabase(
                    userId,
                    new JdbiStorage(jdbi)
                );
            }
        };
    }
}
