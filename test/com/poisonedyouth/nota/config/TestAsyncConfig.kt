package com.poisonedyouth.nota.config

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executor

/**
 * Test configuration that makes async processing synchronous to ensure proper test isolation
 */
@Configuration
@Profile("test")
@EnableAsync
class TestAsyncConfig : AsyncConfigurer {
    override fun getAsyncExecutor(): Executor {
        // Return a synchronous executor for tests to ensure transaction isolation
        return Executor { runnable -> runnable.run() }
    }
}
