package com.aoe4Forum.web.config;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.charset.StandardCharsets;

@Configuration
public class BloomFilterConfig {

    // 预计元素数量（根据实际业务调整）
    private static final long EXPECTED_INSERTIONS = 10_000_000;
    // 误判率（根据业务容忍度调整）
    private static final double FALSE_POSITIVE_PROBABILITY = 0.001;

    @Bean
    public BloomFilter<String> followBloomFilter() {
        return BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                EXPECTED_INSERTIONS,
                FALSE_POSITIVE_PROBABILITY
        );
    }
}