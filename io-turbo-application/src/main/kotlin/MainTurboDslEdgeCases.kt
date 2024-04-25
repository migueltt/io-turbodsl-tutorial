/*
 * Copyright 2024. Project's Author and/or Contributors.
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
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() {
    measure(5) {
        MainTurboDslEdgeCases.main()
    }
}

/** Example using `TurboDSL`:
 * - Execute 3 jobs (job1, job2, job3) in parallel
 * - Execute a final job (job4) that will process the results of all previous 3
 *
 * This implementation adds
 *
 * But, the problem arises when considering edge-cases:
 * - What happens if one of the first 3 jobs fails?
 *   - Should we cancel everything?
 *   - Should we continue using only the jobs that were successful?
 *   - Is there a default-value in case of failures?
 *   - Should we retry the failing job?
 * - Since we do not want to wait forever, a timeout limit should be set.
 *   - It may be that job1 is calling an API-endpoint or reading a file - IO operations are expensive.
 *   - Should a timeout limit imposed on each job?
 *   - Or, should a total timeout limit imposed on all 3 jobs total execution time?
 *   - The same questions as before: what happens if one of the jobs fails?
 *
 * Yes, it is possible to make changes on the code below, but it will get more complex:
 * - What if you need to add one more job due to new requirements?
 * - What if based on business requirements you must ensure that the processing takes place no longer than 5 seconds?
 *
 * There could be several new requirements in the future, and applying changes will become more complex over time.
 *
 * Using the standard coroutine library Kotlin provides:
 * - [runBlocking]
 * - [async]
 * - [kotlinx.coroutines.Deferred.await]
 *
 * Running this 5 times:
 * ```
 * Iteration: 1 -> 5077ms
 * Iteration: 2 -> 5011ms
 * Iteration: 3 -> 5010ms
 * Iteration: 4 -> 5012ms
 * Iteration: 5 -> 5007ms
 * ```
 */
object MainTurboDslEdgeCases {
    fun main() {
        TurboScope.execute<String> {
            async(
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
