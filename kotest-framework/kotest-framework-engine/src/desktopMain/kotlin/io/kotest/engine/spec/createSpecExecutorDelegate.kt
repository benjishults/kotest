package io.kotest.engine.spec

import io.kotest.common.ExperimentalKotest
import io.kotest.core.concurrency.CoroutineDispatcherFactory
import io.kotest.core.config.Configuration
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.engine.listener.TestEngineListener
import io.kotest.engine.test.TestCaseExecutor
import io.kotest.engine.test.listener.TestCaseExecutionListenerToTestEngineListenerAdapter
import io.kotest.engine.test.registration.DuplicateNameHandlingRegistration
import io.kotest.engine.test.registration.FailFastRegistration
import io.kotest.engine.test.registration.InOrderRegistration
import io.kotest.mpp.log

@ExperimentalKotest
internal actual fun createSpecExecutorDelegate(
   listener: TestEngineListener,
   defaultCoroutineDispatcherFactory: CoroutineDispatcherFactory,
   configuration: Configuration,
): SpecExecutorDelegate =
   DefaultSpecExecutorDelegate(listener, defaultCoroutineDispatcherFactory, configuration)

/**
 * A [SpecExecutorDelegate] that executes tests sequentially, using the calling thread
 * as the execution context for timeouts.
 */
@ExperimentalKotest
internal class DefaultSpecExecutorDelegate(
   listener: TestEngineListener,
   private val coroutineDispatcherFactory: CoroutineDispatcherFactory,
   private val configuration: Configuration
) : SpecExecutorDelegate {

   private val materializer = Materializer(configuration)
   private val testCaseListener = TestCaseExecutionListenerToTestEngineListenerAdapter(listener)

   override suspend fun execute(spec: Spec): Map<TestCase, TestResult> {
      log { "DefaultSpecExecutorDelegate: Executing spec $spec" }
      materializer.materialize(spec)
         .forEach { testCase ->
            log { "DefaultSpecExecutorDelegate: Executing testCase $testCase" }
            TestCaseExecutor(
               testCaseListener,
               coroutineDispatcherFactory,
               configuration,
               FailFastRegistration(
                  testCaseListener,
                  configuration,
                  DuplicateNameHandlingRegistration(
                     testCase.spec.duplicateTestNameMode ?: configuration.duplicateTestNameMode,
                     InOrderRegistration(testCaseListener, coroutineDispatcherFactory, configuration)
                  ),
               )
            ).execute(testCase)
         }
      return emptyMap()
   }
}
