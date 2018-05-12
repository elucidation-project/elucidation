package com.fortitudetec.elucidation.server;

import com.fortitudetec.elucidation.server.db.ConnectionEventDao;
import com.fortitudetec.elucidation.server.resources.RelationshipResource;
import com.fortitudetec.elucidation.server.service.RelationshipService;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

public class App extends Application<AppConfiguration> {

    public static void main(final String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public String getName() {
        return "Elucidation Server";
    }

    @Override
    public void initialize(Bootstrap<AppConfiguration> bootstrap) {

        bootstrap.addBundle(new MigrationsBundle<AppConfiguration>() {

            @Override
            public PooledDataSourceFactory getDataSourceFactory(AppConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }

        });
    }

    @Override
    public void run(AppConfiguration configuration, Environment environment) {
        Jdbi jdbi = setupJdbi(configuration, environment);

        ConnectionEventDao dao = jdbi.onDemand(ConnectionEventDao.class);

        RelationshipService relationshipService = new RelationshipService(dao);

        environment.jersey().register(new RelationshipResource(relationshipService));
    }

    private Jdbi setupJdbi(AppConfiguration configuration, Environment environment) {
        JdbiFactory factory = new JdbiFactory();
        Jdbi jdbi = factory.build(environment, configuration.getDataSourceFactory(), "Elucidation-Data-Source");
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }

}
