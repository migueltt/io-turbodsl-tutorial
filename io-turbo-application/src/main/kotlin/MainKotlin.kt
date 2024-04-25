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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/** Run [MainKotlin.main] 5 times. */
fun main() {
    measure(5) {
        MainKotlin.main()
    }
}

/** Example using pure Kotlin:
 * - Execute 3 jobs (job1, job2, job3) in parallel
 * - Execute a final job (job4) that will process the results of all previous 3
 *
 * This could be seen as a trivial problem, since Kotlin provides well-known components to achieve this easily,
 * through the coroutines library.
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
 * Iteration: 1 -> 5055ms // first always have more overhead due to JVM initialization
 * Iteration: 2 -> 5004ms
 * Iteration: 3 -> 5006ms
 * Iteration: 4 -> 5006ms
 * Iteration: 5 -> 5008ms
 * ```
 * Total duration may vary depending on the hardware.
 * Ideally, the whole process should take 5000ms, but there always a tiny overhead due to how
 * coroutines are managed internally, plus the additional statements/expressions added.
 */
object MainKotlin {
    fun main() {
        runBlocking(context = Dispatchers.IO) {
            // Launch two coroutines with async
            val job1 =
                async {
                    // Simulate a long-running task that will calculate some Int value
                    printTest("start job1")
                    delay(1_000L)
                    printTest("finish job1")
                    return@async 100
                }
            val job2 =
                async {
                    // Simulate a long-running task that will calculate some String value
                    printTest("start job2")
                    delay(3_000L)
                    printTest("finish job2")
                    return@async "test"
                }
            val job3 =
                async {
                    // Simulate a long-running task that will calculate some Boolean value
                    printTest("start job3")
                    delay(2_000L)
                    printTest("finish job3")
                    return@async true
                }
            // At this point, both job1 and job2 are started an executed
            printTest("awaiting results")
            // Wait for both results concurrently (non-blocking)
            val value1 = job1.await()
            printTest("job1 results ready")
            val value2 = job2.await()
            printTest("job2 results ready")
            val value3 = job3.await()
            printTest("job3 results ready")
            printTest("all results ready - proceeding to final processing")

            // Process results
            // Simulate a long-running task that will process results and return a String value
            printTest("start job4")
            delay(2_000L)
            printTest("finish job4")
            printTest("Final: $value1 - $value2 - $value3")
        }
    }
}
