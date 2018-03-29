package com.fortitudetec.elucidation.server.db;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.rules.ExternalResource;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class H2JDBIRule extends ExternalResource {

    @Getter
    private Jdbi jdbi;

    @Getter
    private Handle handle;

    @Override
    protected void before() {
        Environment environment = new Environment("test-env", Jackson.newObjectMapper(), null, new MetricRegistry(), null);
        DataSourceFactory dataSourceFactory = getDataSourceFactory();
        jdbi = new JdbiFactory().build(environment, dataSourceFactory, "test");
        handle = jdbi.open();
        createDatabase(dataSourceFactory);
    }

    private void createDatabase(DataSourceFactory dataSourceFactory) {
        ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "migrations");
        try (Connection connection = dataSource.getConnection()) {
            Liquibase migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        } catch (LiquibaseException | SQLException e) {
            log.error("Unable to migrate db", e);
        }
    }

    @Override
    protected void after() {
        handle.close();
    }

    private DataSourceFactory getDataSourceFactory() {
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setDriverClass("org.h2.Driver");
        dataSourceFactory.setUrl(String.format(
            "jdbc:h2:mem:test-%s;MODE=MySQL;TRACE_LEVEL_FILE=3",
            System.currentTimeMillis()));
        dataSourceFactory.setUser("sa");
        return dataSourceFactory;
    }
}
