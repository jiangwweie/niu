package com.xiaoniu.aftermarket;

import javax.sql.DataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Re-runs data.sql before each test class to restore any data modified by previous tests.
 * This prevents test ordering issues with the shared H2 in-memory database.
 */
public class DataResetTestExecutionListener extends AbstractTestExecutionListener {

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        DataSource dataSource = testContext.getApplicationContext().getBean(DataSource.class);
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("data.sql"));
        }
    }
}
