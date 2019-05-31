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

import com.github.mtakaki.dropwizard.admin.AdminResourceBundle;
import com.wire.bots.hold.internal.HoldMessageResource;
import com.wire.bots.hold.model.Config;
import com.wire.bots.hold.resource.*;
import com.wire.bots.hold.utils.HoldClientRepo;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.Server;
import com.wire.bots.sdk.tools.AuthValidator;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class Service extends Server<Config> {
    public static Service instance;
    private final AdminResourceBundle admin = new AdminResourceBundle();

    private Database database;

    public static void main(String[] args) throws Exception {
        instance = new Service();
        instance.run(args);
    }

    @Override
    public void initialize(Bootstrap<Config> bootstrap) {
        super.initialize(bootstrap);

        bootstrap.addBundle(new AssetsBundle("/legalhold/assets/"));
        bootstrap.addBundle(admin);

        Application<Config> application = bootstrap.getApplication();
        instance = (Service) application;
    }

    @Override
    protected void initialize(Config config, Environment env) {
        env.jersey().setUrlPattern("/legalhold/*");

        database = new Database(config.storage);
    }

    @Override
    protected void onRun(Config config, Environment env) {
        RegisterDeviceResource registerDeviceResource = new RegisterDeviceResource(client,
                database,
                getCryptoFactory(),
                config);

        addResource(registerDeviceResource, env);

        AuthValidator validator = new AuthValidator(config.auth);

        addResource(new InitiateResource(getCryptoFactory(), validator), env);
        addResource(new ConfirmResource(database, validator), env);
        addResource(new ListingResource(database, getCryptoFactory()), env);
        addResource(new RemoveResource(database, validator), env);

        admin.getJerseyEnvironment().register(new SettingsResource());
        admin.getJerseyEnvironment().register(new HoldMessageResource(new MessageHandler(database), new HoldClientRepo(getCryptoFactory())));

        Thread thread = new Thread(new NotificationProcessor(client, database));
        thread.start();
    }

    @Override
    protected MessageHandlerBase createHandler(Config config, Environment env) {
        return null;
    }

    @Override
    protected void messageResource(Config config, Environment env, MessageHandlerBase handler, ClientRepo repo) {
    }

    @Override
    protected void botResource(Config config, Environment env, MessageHandlerBase handler) {
    }
}
