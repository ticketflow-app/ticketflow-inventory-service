package com.inventory.service.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the periodic cleanup of expired Redis keys. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
