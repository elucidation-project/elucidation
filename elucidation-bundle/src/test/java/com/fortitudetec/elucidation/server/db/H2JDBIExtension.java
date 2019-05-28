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

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.Connection;

@Slf4j
public class H2JDBIExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final String DB_URL_FORMATTER = "jdbc:h2:mem:test-%s";

    @Getter
    private Jdbi jdbi;

    @Getter
    private Handle handle;

    private static Liquibase liquibase;
    private static JdbcDataSource dataSource;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL(String.format(DB_URL_FORMATTER, System.currentTimeMillis()));

        Connection conn = dataSource.getConnection();
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        liquibase.update(new Contexts());

        jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());

        handle = jdbi.open();

        context.getTestInstance().ifPresent(instance -> {
            try {
                instance.getClass().getMethod("setUp", Jdbi.class).invoke(instance, jdbi);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void afterEach(ExtensionContext context) throws DatabaseException {
        liquibase.dropAll();

        handle.close();
    }

}
