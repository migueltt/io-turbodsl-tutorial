import io.turbodsl.core.TurboScope
import io.turbodsl.core.logging.LoggerProvider
import io.turbodsl.core.scopes.AsyncScope
import io.turbodsl.core.scopes.RuntimeScope
import kotlinx.coroutines.delay
import java.math.BigDecimal
import kotlin.system.measureTimeMillis

/** Run [MainTurboDsl.main] 5 times. */
fun main() {
    System.setProperty(LoggerProvider.PROPERTY_LOGGING_LEVEL, "off")
    System.setProperty(LoggerProvider.PROPERTY_LOGGING_INTERNAL, "true")
    measure(5) {
        MainTurboDsl.main()
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
 * coroutines are managed internally, plus the additional statements/expressions added.
 */
object MainTurboDsl {
    fun main() {
        TurboScope.execute<String> {
            async(
                job1 = asyncJob<Int>(name = "job1") {
                    // Simulate a long-running task that will calculate some Int value
                    printTest("start $name")
                    delay(1_000L)
                    printTest("finish $name")
                    100
                },
                job2 = asyncJob<String>(name = "job2") {
                    // Simulate a long-running task that will calculate some String value
                    printTest("start $name")
                    delay(3_000L)
                    printTest("finish $name")
                    "test"
                },
                job3 = asyncJob<Boolean>(name = "job3") {
                    // Simulate a long-running task that will calculate some Boolean value
                    printTest("start $name")
                    delay(2_000L)
                    printTest("finish $name")
                    true
                }
            ) { ok, r1, r2, r3 ->
                job<Int> { 9
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

fun test() {
    data class Invoice(val total: BigDecimal)
    data class Customer(val credit: BigDecimal)
    data class Discounts(val amount: BigDecimal)
    TurboScope.execute<Unit>(
        retryMode = RuntimeScope.RetryMode.Always,
        retry = 2,
    ) {
        async(
            asyncMode = AsyncScope.AsyncMode.CancelNone,
            job1 = asyncJob<Boolean>(
                retryMode = RuntimeScope.RetryMode.OnTimeoutOnly,
                retry = 2,
                retryDefault = false
            ) {
                job<Boolean> {true } && job<Boolean>{ false }
            },
            job2 = asyncJob<java.math.BigDecimal> {
                async(
                    job1 = asyncJob<Invoice>{ Invoice(BigDecimal("10")) },
                    job2 = asyncJob<Customer>{ Customer(BigDecimal("10"))},
                    job3 = asyncJob<Discounts>(
                        retryMode = RuntimeScope.RetryMode.OnErrorOnly,
                        retry = 3,
                        retryDelay = 10L,
                    ) { Discounts(BigDecimal("10"))}
                ) { ok, r1, r2, r3 ->
                    if (ok) {
                        r1.success().total - r2.success().credit - r3.success().amount
                    } else {
                        BigDecimal.ZERO
                    }
                }
            }
        ) { ok, r1, r2 ->
            if (ok) {
                r1.success()
                r2.success()
            }
        }
    }
}

