package com.isearch.text2vectorApp.config;

import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for TokenCountEstimator bean.
 * Provides accurate token counting using Spring AI's JTokkit implementation.
 */
@Configuration
public class TokenCountEstimatorConfig {

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }
}