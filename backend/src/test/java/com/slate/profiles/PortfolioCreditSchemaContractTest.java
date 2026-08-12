package com.slate.profiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PortfolioCreditSchemaContractTest {

    @Test
    void schemaAndIdempotentMigrationDefineCreditNameColumn() throws IOException {
        Path sqlRoot = Path.of(System.getProperty("user.dir"), "..", "sql").normalize();
        String schema = Files.readString(sqlRoot.resolve("01_schema.sql"));
        String migration = Files.readString(sqlRoot.resolve("08_portfolio_credit_name_schema.sql"));

        assertThat(schema).contains("credit_name varchar(120) NULL");
        assertThat(migration)
                .contains("information_schema.columns")
                .contains("column_name = 'credit_name'")
                .contains("ADD COLUMN credit_name varchar(120) NULL")
                .contains("DROP PROCEDURE IF EXISTS add_portfolio_credit_name_column");
    }
}
