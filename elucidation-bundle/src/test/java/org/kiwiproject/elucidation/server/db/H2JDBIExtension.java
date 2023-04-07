package org.kiwiproject.elucidation.server.db;

/*-
 * #%L
 * Elucidation Server
 * %%
 * Copyright (C) 2018 - 2020 Fortitude Technologies, LLC
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

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isAbstract;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.annotation.Testable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
public class H2JDBIExtension implements ParameterResolver, BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final String DB_URL_FORMATTER = "jdbc:h2:mem:test-%s";
    private static final Namespace JDBI_EXTENSION_NAMESPACE = Namespace.create(Jdbi.class);

    private static Liquibase liquibase;
    private static JdbcDataSource dataSource;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL(format(DB_URL_FORMATTER, System.currentTimeMillis()));

        var conn = dataSource.getConnection();
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
        liquibase = new Liquibase("elucidation-migrations.xml", new ClassLoaderResourceAccessor(), database);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        liquibase.update(new Contexts());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // Abort if parameter type is unsupported
        if (isUnsupportedParameterType(parameterContext.getParameter())) {
            return false;
        }

        var executable = parameterContext.getDeclaringExecutable();

        // @Testable is used as a meta-annotation on @Test, @TestFactory, @TestTemplate, etc
        var isTestableMethod = executable instanceof Method && (isAnnotated(executable, Testable.class) || isAnnotated(executable, BeforeEach.class));
        if (!isTestableMethod) {
            throw new ParameterResolutionException(format("Configuration error: cannot resolve Jdbi instances for [%s]. Only test methods are supported",
                    executable));
        }

        Class<?> parameterType = parameterContext.getParameter().getType();
        if (isAbstract(parameterType.getModifiers())) {
            throw new ParameterResolutionException(format("Configuration error: the resoved Jdbi implementation [%s] is abstract and cannot be instantiated",
                    executable));
        }

        return true;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        // The parameter type is guaranteed to be an instance of Jdbi
        return getStore(extensionContext)
                .getOrComputeIfAbsent(
                        Jdbi.class,
                        unused -> createJdbi(),
                        Jdbi.class);
    }

    private static ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        return extensionContext.getStore(JDBI_EXTENSION_NAMESPACE);
    }

    private Jdbi createJdbi() {
        var jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());

        return jdbi;
    }

    @Override
    public void afterEach(ExtensionContext context) throws DatabaseException {
        liquibase.dropAll();
    }

    private static boolean isUnsupportedParameterType(Parameter parameter) {
        Class<?> type = parameter.getType();
        return !Jdbi.class.isAssignableFrom(type);
    }
}
