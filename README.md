```text
 - ..- .-. -... --- -.. ... .-.. - ..- .-. -... --- -.. ... .-..
   ___________       ______
     ___  ___/        __/ /           ________   _____  ___
     __  /__  __ _,___ / /_   ____     __  __ \ / ___/ /  /
  ____  // / / // ___// __ \ / __ \ ____  / / / \ \   /  /
   __  // /_/ // /   / (_/ // (_/ /  __  /_/ /___\ \ /  /___
 _____/ \__,_//_/   /_,___/ \____/ _________//_____//______/
 
 io.turbodsl
 
 - ..- .-. -... --- -.. ... .-.. - ..- .-. -... --- -.. ... .-..
```
# TurboDSL
> A _DSL engine_ to **turbo-charge** your Kotlin development.

## Objectives
- `TurboDSL` will not make your application faster.
- `TurboDSL` will make your development easier - most importantly, whenever you need to write
  asynchronous logic - trigger tasks in parallel.
- Error handling is much simpler and natural.
- Asynchronous execution allows 3 different modes:
    - `AsyncMode.CancelNone`: Continue even if any job fails.
    - `AsyncMode.CancelFirst`: Cancel running jobs as soon as something fails, keeping results from those completed jobs.
    - `AsyncMode.CancelAll`: If one job fails, cancel all other jobs.
- Introduce initial delays, if required.
- Specify timeout (maximum execution time), if required.
- Specify retry strategies:
    - `RetryMode.Never`: Never retry.
    - `RetryMode.OnTimeoutOnly`: Retry only when `timeout` limit has been reached.
    - `RetryMode.OnErrorOnly`: Retry only when an unexpected error was thrown.
    - `RetryMode.Always`: Retry always, for any error.
    - Delays between retries.
    - Default value if last retry is unsuccessful.
- The runtime overhead is minimal, comparing to pure Kotlin coroutines implementation.


For example, just a simple process triggering 3 jobs in parallel.
- job1 takes at least 1s
- job2 takes at least 3s
- job3 takes at least 2s
- final processing for the results from all 3 jobs takes around 2s
- The overall process takes at least 5s

```kotlin
// Using pure Kotlin coroutines
fun main() {
  repeat(5) {
    val iteration = it + 1
    // println("Iteration: $iteration =============================================")
    measureTimeMillis {
      runBlocking(context = Dispatchers.IO) {
        // Launch two coroutines with async
        val job1 = async {
          // Simulate a long-running task that will calculate some Int value
          delay(1_000L)
          return@async 100
        }
        val job2 = async {
          // Simulate a long-running task that will calculate some String value
          delay(3_000L)
          return@async "test"
        }
        val job3 = async {
          // Simulate a long-running task that will calculate some Boolean value
          delay(2_000L)
          return@async true
        }
        // Wait for both results concurrently (non-blocking)
        val value1 = job1.await()
        val value2 = job2.await()
        val value3 = job3.await()
        // Process results
        // Simulate a long-running task that will process results and return a String value
        delay(2_000L)
        // println("Result: $value1 - $value2 - $value3")
      }
    }.let { elapsed ->
      println("Iteration $iteration -> ${elapsed}ms")
    }
  }
}
```
```kotlin
// Using TurboDSL
fun main() {
    repeat(10) {
        val iteration = it + 1
        measureTimeMillis {
            TurboScope.execute<String> {
                async(
                    job1 = asyncJob<Int>(name = "job1") {
                        // Simulate a long-running task that will calculate some Int value
                        delay(1_000L)
                        100
                    },
                    job2 = asyncJob<String>(name = "job2") {
                        // Simulate a long-running task that will calculate some String value
                        delay(3_000L)
                        "test"
                    },
                    job3 = asyncJob<Boolean>(name = "job3") {
                        // Simulate a long-running task that will calculate some Boolean value
                        delay(2_000L)
                        true
                    }
                ) { ok, r1, r2, r3 ->
                    if (ok) {
                        val job1 = r1.success()
                        val job2 = r2.success()
                        val job3 = r3.success()
                        delay(2_000L)
                        "$job1 - $job2 - $job3"
                    } else {
                        "none"
                    }
                }
            }
        }.let { elapsed ->
            println("Iteration $iteration -> ${elapsed}ms")
        }
    }
}
```
Output (actual times may vary depending on hardware)
```text
                        TurboDSL         TurboDSL
                Kotlin  no-logging  Diff  w/logging   Diff
                ======  ==========  ===== =========  =====  
Iteration  1 -> 5037ms      5074ms  +37ms    5099ms  +62ms // First execution always takes more time due to JVM startup
Iteration  2 -> 5004ms      5007ms  + 3ms    5007ms  + 3ms  
Iteration  3 -> 5004ms      5008ms  + 4ms    5012ms  + 8ms
Iteration  4 -> 5003ms      5008ms  + 5ms    5008ms  + 5ms
Iteration  5 -> 5005ms      5008ms  + 3ms    5005ms    0ms
Iteration  6 -> 5004ms      5006ms  + 1ms    5005ms  + 1ms
Iteration  7 -> 5006ms      5006ms    0ms    5008ms  + 2ms
Iteration  8 -> 5006ms      5006ms    0ms    5005ms  - 1ms
Iteration  9 -> 5005ms      5015ms  +10ms    5005ms    0ms
Iteration 10 -> 5012ms      5004ms  - 8ms    5006ms  - 6ms
```
Roughly, there's a difference of 4ms - without considering the fist execution, even with logging enabled.
This is negligible and the benefits `TurboDSL` provides outweighs these runtime differences.


TODO below

Retrieve an Invoice using its number.
```kotlin
  TurboScope.execute {
      // Group several sequential jobs setting a maximum timeout limit
      job(timeout = 1_000L) {
          job { ... }
          job { ... }
          job { ... }
      }
      // Execute several jobs in parallel with a maximum timeout limit
      async(
          // All jobs must be executed in less than a second
          timeout = 2_000L,
          job1 = asyncJob { ... },
          job2 = asyncJob { ... },
          job3 = asyncJob { ... },
          :
      ) { ok, r1, r2, r3, ... ->
          
          job { ... }
      }
  }
```

Retrieve an Invoice using its number.
```kotlin
  data class Customer(val name: String)
  data class InvoiceHeader(val address: String)
  data class InvoiceItem(val address: String)
  data class Invoice(
      val customer: Customer,
      val header: InvoiceHeader,
      val items: List<InvoiceItem>,
  )
  class InvoiceError(msg: String, cause: Throwable?) : Error(msg, cause)
  TurboScope.execute<String, Invoice>(context = "123123") {
      job(name = "verifyInvoiceId") {
          // Network call to SalesAPI - throw error if invalid

      }
      async(
          // Cancel-all jobs if one fails
          asyncMode = AsyncScope.AsyncMode.CancelAll,
          // All jobs must be executed in less than a second
          timeout = 1_000L,
          // Always retry in case of failure
          retryMode = RuntimeScope.RetryMode.Always,
          // ... up to 3 more times
          retry = 3,
          // ... but wait 1 second between retries
          retryDelay = 1_000L,
          job1 = asyncJob<Customer> {
              // Get customer details using parent context (invoiceId)
              Customer("")
          },
          job2 = asyncJob<InvoiceHeader> {
              // Get invoice details (header) using parent context (invoiceId)
             InvoiceHeader("")
          },
          job3 = asyncJob<List<InvoiceItem>> {
              // Get invoice items (details) using parent context (invoiceId)
             emptyList()
          },
      ) { ok, r1, r2, r3 ->
          if (ok) {
              Invoice(
                  customer = r1.success(),
                  header = r2.success(),
                  items = r3.success(),
              )
          } else {
              when {
                  r1.isFailure() -> 
                      throw InvoiceError(
                          msg ="Customer for invoice '$context' cannot be found",
                          cause = r1.failure()
                      )
                  r2.isFailure() ->
                      throw InvoiceError(
                          msg ="Header for invoice '$context' cannot be found",
                          cause = r2.failure()
                      )
                  r3.isFailure() ->
                      throw InvoiceError(
                          msg ="Details for invoice '$context' cannot be found",
                          cause = r2.failure()
                      )
                  else ->
                      // Edge-case: This could only happen if all `r` were cancelled due to `asyncMode`.
                      // This cannot happen, since the "contract" with "CancelAll" will set the `r` as a "Failure" for the first error.
                      // Since compiler requires an `else`, just specifying an impossible branch.
                      throw Error(
                          msg = "Unexpected error",
                          cause = null
                      )
              }
          }
      }
  }
```

For example, imagine the following scenario:
* A process requires 3 steps in parallel:
  * Step 1 may be retried up to 3 times.
  * Step 2 requires Step 1 output but triggers 2 more steps in parallel:
    * Step 2.1 performs a long-running calculation returning a number.
    * Step 2.2 performs a remote-call checking for availability, returning true/false.
  * Step 3 triggers 3 more steps:
    * Step 3.1 uses output from Step 2.1 if and only if Step 2.2 was true. Otherwise, uses a default value.
    * Step 3.2 uses output from Step 2.1 always.
    * Step 3.3 uses output from Step 1 always.
  * Finally, all Step 3 outputs are combined.


```kotlin
//
data class Client(
    val name: String,
    // more members
)
enum class InvoiceStatus {
    ORDERED,
    SHIPPED,
    CANCELLED,
    // more members
}
data class InvoiceItem(
    val product: Product,
    val price: BigDecimal,
    val quantity: Int,
    // more members
)
data class Invoice(
    val client: Client,
    val status: InvoiceStatus,
    val items: List<InvoiceItem>,
    // more members
)
// Extract 
TurboScope.execute<String, Invoice>(context = "123123") {
    async(
        job1 = asyncJob<Client>(name = "getClient") {
            // Use `context` to get the invoice-number 
            // Execute remote call to CRM, returning the client.
            Client("Wile E. Coyote")
        },
        job2 = asyncJob<InvoiceStatus>(name = "getStatus") {
            // Use `context` to get the invoice-number
            // Execute remote call to order-processing, returning the status
            InvoiceStatus.SHIPPED
        },
        job3 = asyncJob<List<InvoiceItem>>(name = "getDetails") {
            // Use `context` to get the invoice-number
            // Execute remote call to sales-system, returning invoice details
            listOf(
                InvoiceItem("Hammer", BigDecimal("20.99"), 2),
                InvoiceItem("Ladder", BigDecimal("45.99"), 1),
            )
        }
    ) { ok, r1, r2, r3 ->
        if (ok) {
            // Everything is ready - create Invoice
            Invoice(r1.success(), r2.success(), r3.success())
        } else {
            // Can't find invoice - just return null
            // Exception requires the invoice-number
            // `context` has the initial value
            throw InvoiceNotFoundException(context)
        }
    }
}.let { invoice ->
    // process invoice
}
```