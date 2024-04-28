/*
 * Copyright 2024 migueltt and/or Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.turbodsl.core.TurboScope
import io.turbodsl.core.logging.LoggerProvider
import kotlinx.coroutines.delay

/** Run [MainTurboDSL.main] 5 times. */
fun main() {
    // Enable internal logging to see what's going on
    System.setProperty(LoggerProvider.PROPERTY_LOGGING_LEVEL, "off")
    System.setProperty(LoggerProvider.PROPERTY_LOGGING_INTERNAL, "true")
    measure(5) {
        MainTurboDSL.main()
    }
}

/** Example using `TurboDSL`:
 * - Execute 3 jobs (job1, job2, job3) in parallel
 * - Execute a final job (job4) that will process the results of all previous 3
 *
 * This implementation achieves the same as [MainKotlin.main], but uses `TurboDSL` expressions.
 *
 * Running this 5 times:
 * ```
 *                 No-log  With-log
 *                 ======= ========
 * Iteration: 1 -> 5079ms    5110ms  // first always have more overhead due to JVM initialization
 * Iteration: 2 -> 5008ms    5010ms
 * Iteration: 3 -> 5009ms    5007ms
 * Iteration: 4 -> 5008ms    5008ms
 * Iteration: 5 -> 5006ms    5009ms
 * ```
 * Total duration may vary depending on the hardware.
 * Ideally, the whole process should take 5000ms, but there always a tiny overhead due to how
 * coroutines are managed internally by Kotlin, plus the additional statements/expressions added.
 */
object MainTurboDSL {
    fun main() {
        TurboScope.execute<String> {
            async(
                name = "async-3-jobs",
                job1 =
                    asyncJob<Int>(name = "job1") {
                        // Simulate a long-running task that will calculate some Int value
                        printTest("start $name")
                        delay(1_000L)
                        printTest("finish $name")
                        100
                    },
                job2 =
                    asyncJob<String>(name = "job2") {
                        // Simulate a long-running task that will calculate some String value
                        printTest("start $name")
                        delay(3_000L)
                        printTest("finish $name")
                        "test"
                    },
                job3 =
                    asyncJob<Boolean>(name = "job3") {
                        // Simulate a long-running task that will calculate some Boolean value
                        printTest("start $name")
                        delay(2_000L)
                        printTest("finish $name")
                        true
                    },
            ) { ok, r1, r2, r3 ->
                job<Int> {
                    9
                } +
                    job<Int> { 10 }

                job(name = "job4", context = this) {
                    with(context) {
                        if (ok) {
                            val job1 = r1.success()
                            val job2 = r2.success()
                            val job3 = r3.success()
                            printTest("all results ready - proceeding to final processing")
                            // Simulate a long-running task that will process results and return a String value
                            printTest("start ${this@job.name}")
                            delay(2_000L)
                            printTest("finish ${this@job.name}")
                            "$job1 - $job2 - $job3"
                        } else {
                            "none"
                        }
                    }
                }
            }
        }.let {
            printTest("Final: $it")
        }
    }
}
