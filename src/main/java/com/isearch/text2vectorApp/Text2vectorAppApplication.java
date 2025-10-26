package com.isearch.text2vectorApp;

import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, PgVectorStoreAutoConfiguration.class})
public class Text2vectorAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(Text2vectorAppApplication.class, args);
    }

}
