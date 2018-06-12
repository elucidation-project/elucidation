package com.fortitudetec.elucidation.server.db;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 Fortitude Technologies, LLC
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

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
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class H2JDBIExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

    @Getter
    private Jdbi jdbi;

    @Getter
    private Handle handle;

    @Override
    public void beforeAll(ExtensionContext context) {
        Environment environment = new Environment("test-env", Jackson.newObjectMapper(), null, new MetricRegistry(), null);
        DataSourceFactory dataSourceFactory = getDataSourceFactory();
        jdbi = new JdbiFactory().build(environment, dataSourceFactory, "test");
        jdbi.installPlugin(new SqlObjectPlugin());
        handle = jdbi.open();
        createDatabase(dataSourceFactory);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        context.getTestInstance().ifPresent(instance -> {
            try {
                instance.getClass().getMethod("setUp", Jdbi.class).invoke(instance, jdbi);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void afterAll(ExtensionContext context) {
        handle.close();
    }

    private void createDatabase(DataSourceFactory dataSourceFactory) {
        ManagedDataSource dataSource = dataSourceFactory.build(new MetricRegistry(), "migrations");
        try (Connection connection = dataSource.getConnection()) {
            Liquibase migrator = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), new JdbcConnection(connection));
            migrator.update("");
        } catch (LiquibaseException | SQLException e) {
            LOG.error("Unable to migrate db", e);
        }
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
