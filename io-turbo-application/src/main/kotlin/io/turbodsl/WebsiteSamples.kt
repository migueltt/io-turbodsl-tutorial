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

package io.turbodsl

import io.turbodsl.collections.asyncFilter
import io.turbodsl.collections.asyncMap
import io.turbodsl.core.Default
import io.turbodsl.core.Default.Companion.toDefault
import io.turbodsl.core.TurboScope
import io.turbodsl.core.scopes.AsyncJobScope
import io.turbodsl.core.scopes.AsyncResult
import io.turbodsl.core.scopes.AsyncScope
import io.turbodsl.core.scopes.RetryScope
import io.turbodsl.core.scopes.allSuccess
import kotlinx.coroutines.delay
import java.math.BigDecimal

/* This file contains all sample code included in https://turbodsl.io
 * Note that all these functions are private.
 * Use `main` to execute all of them.
 */
fun main() {
    sectionHome()
    pageFundamentals()
    pageDefaultMechanisms()
    pageSyncVsAsyncScopes()
    pageAsynchronousResults()
    pageRetryPattern()
    pageCodeReusability()
    pageCollections()
}

private data class Foobar(val test: String)

//region Home
private fun sectionHome() {
    // Restrict all execution to 10 seconds.
    TurboScope.execute(timeout = 10_000L) {
        // Execute 3 jobs in parallel, and retry up to 10 times.
        // Return empty-string after last retry failure.
        // Restrict each retry execution to 2 seconds.
        retry(retries = 10, timeout = 2_000L, default = Default.EmptyString) {
            async(
                // Cancel pending jobs on first failure
                asyncMode = AsyncScope.AsyncMode.CancelFirst,
                throwOnFailure = true,
                job1 = asyncJob<String>(timeout = 500L) { "job1" },
                job2 = asyncJob<String>(delay = 100L) { "job2" },
                job3 = asyncJob<String>(timeout = 1_000L) { "job3" },
            ) { _, r1, r2, r3 ->
                // Since `throwOnFailure` is true, then this block is executed only if all jobs are successful.
                "${r1.success()} ${r2.success()} ${r3.success()}"
            }
        }.let { data ->
            // Retry 3 more times, delaying 0.5, 1, 2 seconds respectively
            retry<String, String>(
                input = data, retries = 3,
                retryDelay = 500, retryDelayFactor = 2.0,
            ) {
                // Maps each character in parallel (0 means trigger as many coroutines as items in collection)
                input.toList().asyncMap(maxJobs = 0) {
                    when (it) {
                        'j' -> 1
                        'o' -> 2
                        'b' -> 3
                        ' ' -> 0
                        else -> it
                    }
                }.joinToString(separator = "-")
            }
        }
    }.let {
        // Prints out: 1-2-3-1-0-1-2-3-2-0-1-2-3-3
        println("Result: $it")
    }
}
//endregion

//region Fundamentals
private fun pageFundamentals() {
    //region DSL Expressions
    TurboScope.execute<String> {
        async(
            name = "async-3-jobs",
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
            },
        ) { ok, r1, r2, r3 ->
            if (ok) {
                val result1 = r1.success()
                val result2 = r2.success()
                val result3 = r3.success()
                // Simulate a long-running task that will process results and return a String value
                job(name = "job4") {
                    delay(2_000L)
                    // we can safely reference result1, result2, result3
                    "$result1 - $result2 - $result3"
                }
            } else {
                "none"
            }
        }
    }.let {
        println("DSL Expressions: $it")
    }
    //endregion
    //region Scope Hierarchy
    TurboScope.execute<Unit> {
        // "this" is an instance of TurboScope
        job {
            // "this"  is an instance of JobScope
        }
        async {
            // "this" is an instance of AsyncRawScope
            asyncJob { // registers an async-job
                // "this" is an instance of AsyncJobScope
            }
        }
        async<Unit>(
            build = {
                // "this" is an instance of AsyncReturnScope
                asyncJob { 1 } // registers an async-job
                asyncJob { 2 } // registers an async-job
            }
        ) { ok, r ->
            // "this" is an instance of AsyncResultScope
        }
        async(
            job1 = asyncJob<Int> { 1 }, // creates an async-job
            job2 = asyncJob<String> { "2" }, // creates an async-job
            // :
            // job10 = asyncJob<Boolean> { true }, // creates an async-job
        ) { ok, r1, r2 -> //, ... r10 ->
            // "this" is an instance of AsyncResultScope
        }
    }
    //endregion
    //region Scopes
    TurboScope.execute {
        // This block executes synchronously.
        job {
            // This block executes synchronously.
        }
        async {
            // This block executes synchronously.
            asyncJob { // Registers an AsyncJobScope
                // This block executes synchronously when
                // the parent `async` decides.
            }
            asyncJob { // Registers an AsyncJobScope
                // This block executes synchronously when
                // the parent `async` decides.
            }
            // Once the execution of this block is completed,
            // `async` will execute concurrently all the
            // registered AsyncJobScopes.
        }
        // `execute` will return List<AsyncResult<*>> since
        // async is the last expression, as inferred by
        // Kotlin compiler.
    }
    //endregion
    //region Input & Output
    TurboScope.execute {
        // nothing to do
    }
    TurboScope.execute {
        "hello world"
    }
    TurboScope.execute(input = true) {
        10.0
    }
    TurboScope.execute {
        job {
            "hello world"
        }.length + job<Int> {
            10
        }
    } // returns 21
    TurboScope.execute<Int, Int>(input = 100) {
        async(
            job1 = asyncJob<String> { "abc" },
            job2 = asyncJob<Int, String>(input = input * 2) { "x".repeat(input) },
        ) { ok, r1, r2 ->
            if (ok) {
                r1.success().length + r2.success().length
            } else {
                throw Error("job1 or job2 failed")
            }
        }
    } // returns 203
    //endregion
    //region Default Mechanisms
    TurboScope.execute(default = Foobar("foo").toDefault()) {
        job<Foobar>(
            defaultFun = {
                // Only executed when scope fails
                Foobar("bar").toDefault()
            }
        ) { Foobar("foobar") }
    }
    //endregion
    //region Timeout Mechanisms
    TurboScope.execute<Unit>(timeout = 2_000L) {
        async(
            timeout = 1_000L,
            job1 = asyncJob<Int>(timeout = 10L) { 10 },
            job2 = asyncJob<Double>(timeout = 10L) { 1.0 },
            job3 = asyncJob<Foobar>(timeout = 10L) { Foobar("bar") },
        ) { ok, r1, r2, r3 ->
            // process results
        }
    }
    //endregion
    //region Initial Delay
    TurboScope.execute<Unit>(
        delay = 1_000L,
        timeout = 500L
    ) {
        // Waits for 1 second before executing.
        // The maximum time to complete this block
        // is 0.5 seconds
        job(delay = 1_000L) {
            // This will fail since this job will wait
            // 1 second before executing since the
            // parent timeout is 0.5 seconds.
        }
    }
    //endregion
    //region Asynchronous Jobs
    TurboScope.execute<Unit> {
        async {
            asyncJob { 1 }
            asyncJob { 2 }
            // register more jobs as required
        }.let { r ->
            // async returns a list
        }
        async<Unit>(
            build = {
                asyncJob { 1 }
                asyncJob { 2 }
                // register more jobs as required
            }
        ) { ok, r ->
            // if ok==true, r contains all successes
        }
        async(
            job1 = asyncJob<Int>{ 1 },
            job2 = asyncJob<String>{ "2" },
            job3 = asyncJob<Foobar>{ Foobar("bar")},
            // register up to 10 jobs as required
        ) { ok, r1, r2, r3 ->
            // if ok==true, r1, r2, r3 are successes
            // `r1` contains job1 result as Int
            // `r2` contains job2 result as String
            // `r3` contains job3 result as Foobar
        }
    }
    //endregion
    //region Retry Mechanisms
    TurboScope.execute(timeout = 500L) {
        retry<String>(
            retryMode = RetryScope.RetryMode.OnErrorOnly,
            retries = 3, // retry 3 more times
            retryDelay = 10L, // wait between retries
            default = "NONE".toDefault(),
        ) {
            // This block could be executed 4 times.
            // :
            // If the last retry fails, return default.
            "foobar"
        }
    }
    //endregion
}
//endregion

//region Default Mechanisms
private fun pageDefaultMechanisms() {
    //region Fail-Safe Patterns
    // Traditional approach
    fun doSomething(): String =
        try {
            // operations to perform
            "hello world"
        } catch (e: Throwable) {
            // log `e`, and return a default value
            "foobar"
        }
    // io.turbodsl approach
    fun doSomething1(): String =
        TurboScope.execute<String>(default = "foobar".toDefault()) {
            // operations to perform
            // :
            "hello world"
        }
    //endregion
    //region Default Parameters
    TurboScope.execute {
        val tmp = job<String>(
            default = Default.Defined("Foo")
        ) {
            "hello world"
        }
        // tmp = "Foo"
    }
    // Main entry point for execution
    fun main() {
        val tmp = TurboScope.execute<String>(
            defaultFun = {
                // "this" refers to `TurboScope
                "Bar".toDefault()
            }
        ) {
            // "this" refers to `TurboScope
            throw Error()
        }
        // tmp is "Bar"
    }
    // Anywhere within your codebase
    TurboScope.execute {
        // "this" refers to `TurboScope`
        val tmp = job<Foobar>(
            defaultFun = {
                // "this" refers to `JobScope`
                Foobar("foo").toDefault()
            }
        ) {
            // "this" refers to `JobScope`
            throw Error()
        }
        // tmp is Foobar("foo")
    }
    //endregion
    //region Default values are optional
    fun <T, R> test(input: T, block: (T) -> R): R = block.invoke(input)
    val foo: String? = test<Int, String?>(input = 10) {
        // perform some calculation returning a `String?`
        ""
    }
    val bar: Boolean? = test<String?, Boolean?>(input = foo) {
        when {
            it.isNullOrBlank() -> null
            it.length > 10 -> true
            else -> false
        }
    }
    //endregion
    //region Default Constants
    TurboScope.execute {
        job(default = Default.Unit) { Unit }
        job(default = Default.True) { true }
        job(default = Default.False) { false }
        job(default = Default.EmptyString) { "foo"}
        job(default = Default.Zero) { 100 }
        job(default = Default.One) { 100 }
        job(default = Default.MinusOne) { 100 }
        job(default = if (true) Default.False else Default.Undefined) { true }
        job<Boolean>(defaultFun = { Default.True }) { true }
        job<Boolean>(defaultFun = { Default.False }) { false }
        job<Boolean>(defaultFun = {
            val condition: Boolean = async(
                job1 = asyncJob<Foobar> { Foobar("bar") },
                job2 = asyncJob<String> { "foo" },
            ) { ok, r1, r2 ->
                // calculate condition based on r1 and r2
                true
            }
            // If the condition is true, then there's a default.
            // Otherwise, let the main block fail.
            if (condition) Default.False else Default.Undefined
        }
        ){
            true
        }
    }
    TurboScope.execute {
        job<Foobar?>(default = null.toDefault()) { null }
        job<Foobar?>(defaultFun = {
            job<Foobar?> {
                // Retrieve some Foo value using some default mechanisms.
                // If nothing was found, returns null.
                null
            }.toDefault()
        }) { null }
    }
    //endregion
}
//endregion

//region Sync vs. Async Scopes
private fun pageSyncVsAsyncScopes() {
    //region SyncScope
    TurboScope.execute<Unit> {
        // "this" is an instance of TurboScope
        job {
            // "this" is an instance of JobScope
        }
        asyncJob {
            // "this" is an instance of AsyncJobScope
        }
        async {
            // "this" is an instance of AsyncRawScope
        }
        async<Unit>(
            build = {
                // "this" is an instance of AsyncReturnScope
            }
        ) { ok, r ->
            // "this" is an instance of AsyncResultScope
        }
        //async(job1 = ..., job2 = ..., ... , job10 = ...) { ok, r1, r2, ..., r10 ->
        //    // "this" is an instance of AsyncResultScope
        //}
        retry {
            // "this" is an instance of RetryScope
        }
    }
    data class SomeData(val flag: Boolean)
    TurboScope.execute<Boolean> {
        job(input = job<List<SomeData>> {
                /* returns a List<SomeData> */
                emptyList()
            }
        ) {
            // Using Kotlin's collection extension function
            input.partition { it.flag }.let { (fTrue, fFalse) ->
                job(input = fTrue) {
                    // Calculate and return a BigDecimal
                    BigDecimal(10)
                }.add(
                    job(input = fFalse) {
                        // Calculate and return a BigDecimal
                        BigDecimal(10)
                    }
                )
            }
        } > BigDecimal.valueOf(1_000)
    }
    //endregion
    //region AsyncScope - unknown
    TurboScope.execute {
        async {
            // "this" is an instance of AsyncRawScope
            asyncJob<Int>{ 1 }
            asyncJob<Foobar>{ Foobar("foo") }
            repeat(job { 1 } ) {
                asyncJob<Double>{ 10.0 }
            }
        }.let { r ->
            // r is a List<AsyncResult<*>> - size could vary between 2 and 102 items
        }
    }
    TurboScope.execute {
        // `results` is a List<AsyncResult<*>> - size could vary between 2 and 102 items
        val results = async(maxJobs = -102) { // up to 102 asyncJobs
            // "this" is an instance of AsyncRawScope
            if (true /* <some-condition> */) {
                // Warning! results structure is different depending on <some-condition>
                asyncJob<Int>{ 1 }
                asyncJob<Foobar>{ Foobar("bar") }
            }
            repeat(job { 1 /* returns an Int between 0 and 100 */ } ) {
                asyncJob<Double>{ 10.0 }
            }
        }
    }
    //endregion
    //region AsyncScope - known
    data class Summary(val text: String = "")
    data class Employee(val text: String = "")
    data class Invoice(val text: String = "")
    data class Item(val text: String = "")
    data class Registration(val text: String = "", val valid: Boolean = false)
    TurboScope.execute<Summary> {
        async(
            job1 = asyncJob<Employee>{ Employee() },
            job2 = asyncJob<List<Invoice>>{ emptyList() },
            // Note that depending on `condition` the asyncJob implementation
            // may be different, but the return-type is always the same.
            job3 = if (true /* <some-condition>*/ ) asyncJob<List<Item>>{ emptyList() } else asyncJob<List<Item>>{ emptyList() },
            // Similar to job3, but in this case, the condition is within the implementation.
            // There may be situations that you may want to use the "job3" approach (e.g. different timeouts)
            job4 = asyncJob<Registration, List<Item>>(input = Registration()){
                if (input.valid) { emptyList() } else { emptyList() }
            },
        ) { ok, r1, r2, r3, r4 ->
            // "this" is an instance of AsyncResultScope
            Summary()
        }
    }
    //endregion
}
//endregion

//region Asynchronous Results
private fun pageAsynchronousResults() {
    //region Evaluating raw AsyncResults
    TurboScope.execute {
        async {
            // adding 3 jobs for execution
            asyncJob<Int>{ 1 }
            asyncJob<Boolean>{ true }
            asyncJob<Double>{ 3.0 }
        }.let { r ->
            when (r[0]) {
                AsyncResult.Cancelled -> Unit
                is AsyncResult.Completed.Success -> Unit
                is AsyncResult.Completed.Failure -> Unit
            }
            when (r[1]) {
                AsyncResult.Cancelled -> Unit
                is AsyncResult.Completed.Success -> Unit
                is AsyncResult.Completed.Failure -> Unit
            }
            when (r[2]) {
                AsyncResult.Cancelled -> Unit
                is AsyncResult.Completed.Success -> Unit
                is AsyncResult.Completed.Failure -> Unit
            }
        }
    }
    TurboScope.execute {
        async {
            // adding 3 jobs for execution
            asyncJob<Int>{ 1 }
            asyncJob<Boolean>{ true }
            asyncJob<Double>{ 3.0 }
        }.let { r ->
            if (r.allSuccess()) {
                // all r items are succeses
            } else {
                // handler errors / cancellations
            }
        }
    }
    TurboScope.execute {
        async {
            // adding 3 jobs for execution
            asyncJob<Int>{ 1 }
            asyncJob<Boolean>{ true }
            asyncJob<Double>{ 3.0 }
        }.let { r ->
            when {
                r[0].isSuccess() -> { Unit }
                r[0].isFailure() -> { Unit }
                r[0].isCancelled() -> { Unit }
            }
            Unit
        }
    }
    TurboScope.execute {
        async {
            // adding 3 jobs for execution
            asyncJob<Int>{ 1 }
            asyncJob<Boolean>{ true }
            asyncJob<Double>{ 3.0 }
        }.let { r ->
            when (val r0 = r[0]) {
                AsyncResult.Cancelled -> {
                    // r0 does not provide any reference to the determine state
                }
                is AsyncResult.Completed.Success ->  {
                    // r0.value -> requires casting
                }
                is AsyncResult.Completed.Failure ->  {
                    // r0.error -> it's a Throwable
                }
            }
            Unit
        }
    }
    TurboScope.execute {
        async {
            // adding 3 jobs for execution
            asyncJob<Int>{ 1 }
            asyncJob<Boolean>{ true }
            asyncJob<Double>{ 3.0 }
        }.let { r ->
            if (r.allSuccess()) {
                // Safe to cast to Success, since all are Successes
                // But, you need to know the data-type for each r[index]
                val r0 = ((r[0] as AsyncResult.Completed.Success).value as Int)
                val r1 = ((r[1] as AsyncResult.Completed.Success).value as Boolean)
                val r2 = ((r[2] as AsyncResult.Completed.Success).value as Double)
                // Process results
            } else {
                // handler errors / cancellations
                // At least one r[i] is an AsyncResult.Completed.Failure
            }
        }
    }
    //endregion
    //region Evaluating results using AsyncResultScope
    TurboScope.execute {
        async<String>(
            build = {
                // "this" is an instance of AsyncScope
                // adding 3 jobs for execution
                asyncJob<Int>{ 1 }
                asyncJob<Boolean>{ true }
                asyncJob<Double>{ 3.0 }
            },
        ) { ok, r ->
            // "this" is an instance of AsyncResultScope
            if (ok) {
                // Safe to cast to Success, since all are Successes
                // But, you need to know the data-type for each r[index]
                val r0 = (r[0].success() as Int)
                val r1 = (r[1].success() as Boolean)
                val r2 = (r[2].success() as Double)
                // Process results and return a String value
            } else {
                // handler errors / cancellations
                // At least one r[i] is an AsyncResult.Completed.Failure
                // Throw error, or return a String value
            }
            ""
        }
    }
    //endregion
    //region Receiving each result using AsyncResultScope
    TurboScope.execute<String> {
        async(
            // adding 3 jobs for execution
            job1 = asyncJob<Int>{ 1 },
            job2 = asyncJob<Boolean>{ true },
            job3 = asyncJob<Double>{ 3.0 },
        ) { ok, r1, r2, r3 ->
            // "this" is an instance of AsyncResultScope
            if (ok) {
                // No need to cast to Success, and safe to call success()
                // No need to cast to data-type - each r-index "knows" the type to cast into
                val r0 = r1.success() // r0 is an Int
                val r1 = r2.success() // r1 is a Boolean
                val r2 = r3.success() // r2 is a Double
                // Process results and return a String value
            } else {
                // handler errors / cancellations
                // At least one r[i] is an AsyncResult.Completed.Failure
            }
            ""
        }
    }
    //endregion
}
//endregion

//region Retry Pattern
private fun pageRetryPattern() {
    //region Retryable Operations
    // Returns a String
    TurboScope.execute {
        // Retry 5 times whenever AsyncJobs take more than 2 seconds.
        // Any other error will be thrown.
        // Delay each retry by 1, 2, 4, 8, 16 seconds respectively.
        // retry#1: 1_000L * 2_000L.pow(1 - 1) ->  1_000L
        // retry#2: 1_000L * 2_000L.pow(2 - 1) ->  2_000L
        // retry#3: 1_000L * 2_000L.pow(3 - 1) ->  4_000L
        // retry#4: 1_000L * 2_000L.pow(4 - 1) ->  8_000L
        // retry#5: 1_000L * 2_000L.pow(5 - 1) -> 16_000L
        // If last retry fails on timeout, return "INVALID"
        retry(
            retryMode = RetryScope.RetryMode.OnTimeoutOnly,
            retries = 5,
            timeout = 2_000L,
            retryDelay = 1_000,
            retryDelayFactor = 2.0,
            default = "INVALID".toDefault()
        ) {
            // Any Kotlin expressions / statements
            // Or even additional io.turbodsl expressions
            job { 1 }
            job { 2 }
            ""
        }
    }
    //endregion
    //region Retrying Asynchronous Code
    TurboScope.execute<Unit> {
        async {
            retry { // deprecated / not-allowed
                asyncJob { 1 }
                asyncJob { 2 }
                // :
            }
        }
        async(
            build = {
                retry { // deprecated / not-allowed
                    asyncJob { 1 }
                    asyncJob { 2 }
                    // :
                }
            }
        ) { ok, r ->
            // :
        }
    }
    TurboScope.execute<Unit> {
        async {
            // Each asyncJob has different requirements for retry.
            asyncJob<Foobar> {
                retry( /* ... */ ) { Foobar("foo") }
            }
            asyncJob { 1 }
            asyncJob<Foobar> {
                retry(/*...*/) { Foobar("foo") }
            }
            // :
        }
        async(
            build = {
                // Each asyncJob has different requirements for retry.
                asyncJob<Foobar> {
                    retry(/*...*/) { Foobar("foo") }
                }
                asyncJob { /*...*/ }
                asyncJob<Foobar> {
                    retry(/*...*/) { Foobar("foo") }
                }
                // :
            }
        ) { ok, r ->
            // :
        }
    }
    TurboScope.execute<Unit> {
        retry(/*...*/) {
            async(
                // Each asyncJob has different requirements for retry.
                job1 = asyncJob<Int> { 1 },
                job2 = asyncJob<Foobar> { Foobar("foo") },
                job3 = asyncJob<Boolean> {
                    retry(/*...*/) { true }
                },
                job4 = asyncJob<String> {
                    retry(/*...*/) { "" }
                },
            ) { ok, r1, r2, r3, r4 ->
                // :
            }
        }
    }
    //endregion
}
//endregion

//region Code Reusability
private fun pageCodeReusability() {
    //region Code Segmentation
    TurboScope.execute<Unit>(name = "main-task") {
        async(
            name = "async - step 1",
            job1 = asyncJob<String>(name = "step 1a") { "step 1a" },
            job2 =
                asyncJob<String>(name = "step 1b") {
                    async(
                        name = "async - step 2",
                        job1 = asyncJob<String>(name = "step 2a") { "step 2a" },
                        job2 =
                            asyncJob<String>(name = "step 2b") {
                                job {
                                    var result = ""
                                    do {
                                        async(
                                            name = "async - step 3",
                                            job1 = asyncJob<String, String>(name = "step 3a", input = result) { input + "3a" },
                                            job2 = asyncJob<String, String>(name = "step 3b", input = result) { input + "3b" },
                                        ) { ok, r1, r2 ->
                                            if (ok) {
                                                r1.success() + r2.success()
                                            } else {
                                                throw when {
                                                    r1.isFailure() -> r1.failure()
                                                    else -> r2.failure()
                                                }
                                            }
                                        }.let {
                                            result += it
                                        }
                                    } while (
                                        job<String, Boolean>(name = "step 4", input = result) {
                                            result.length < 20
                                        }
                                    )
                                    result
                                }
                            },
                    ) { ok, r1, r2 ->
                        if (ok) {
                            job(name = "step 5", input = r1.success() + r2.success()) { input + "step 5" }
                        } else {
                            throw when {
                                r1.isFailure() -> r1.failure()
                                else -> r2.failure()
                            }
                        }
                    }
                },
        ) { ok, r1, r2 ->
            if (ok) {
                job<String, String>(name = "step 6", input = r1.success() + r2.success()) { input + "step 6" }
            } else {
                throw when {
                    r1.isFailure() -> r1.failure()
                    else -> r2.failure()
                }
            }
        }.let {
            job(name = "step 7", input = it) { input + "step 7" }
        }
    }
    //endregion
    //region Code Extraction
    TurboScope.execute<Unit>(name = "main-task") {
        // Local-function defining step-2b
        suspend fun AsyncJobScope<Unit, String>.asyncStep2b(): String =
            job {
                var result = ""
                do {
                    async(
                        name = "async - step 3",
                        job1 = asyncJob<String, String>(name = "step 3a", input = result) { input + "3a" },
                        job2 = asyncJob<String, String>(name = "step 3b", input = result) { input + "3b" },
                    ) { ok, r1, r2 ->
                        if (ok) {
                            r1.success() + r2.success()
                        } else {
                            throw when {
                                r1.isFailure() -> r1.failure()
                                else -> r2.failure()
                            }
                        }
                    }.let {
                        result += it
                    }
                } while (
                    job<String, Boolean>(name = "step 4", input = result) { result.length < 20 }
                )
                result
            }

        // Local-function defining step-2
        suspend fun AsyncJobScope<Unit, String>.asyncStep2(): String =
            async(
                name = "async - step 2",
                job1 = asyncJob<String>(name = "step 2a") { "step 2a" },
                job2 = asyncJob<String>(name = "step 2b") { asyncStep2b() },
            ) { ok, r1, r2 ->
                if (ok) {
                    job(name = "step 5", input = r1.success() + r2.success()) { input + "step 5" }
                } else {
                    throw when {
                        r1.isFailure() -> r1.failure()
                        else -> r2.failure()
                    }
                }
            }
        // Our refactored code
        async(
            name = "async - step 1",
            job1 = asyncJob<String>(name = "step 1a") { "step 1a" },
            job2 = asyncJob<String>(name = "step 1b") { asyncStep2() },
        ) { ok, r1, r2 ->
            if (ok) {
                job<String, String>(name = "step 6", input = r1.success() + r2.success()) { input + "step 6" }
            } else {
                throw when {
                    r1.isFailure() -> r1.failure()
                    else -> r2.failure()
                }
            }
        }.let {
            job(name = "step 7", input = it) { input + "step 7" }
        }
    }
    //endregion
}
//endregion

private data class Employee(val text: String = "", val salary: Int = 1)
private data class Benefit(val text: String = "", val salary: Int = 1)
private data class Timesheet(val text: String = "", val salary: Int = 1)
private sealed class Compensation(open val employee: Employee) {
    data class Eligible(
        override val employee: Employee,
        val benefit: Benefit,
        val timesheet: Timesheet,
    ) : Compensation(employee)
    data class NonEligible(override val employee: Employee) : Compensation(employee)
}
//region Collections
private fun pageCollections() {
    //region Considerations
    val items: List<String> = listOf(1, 2, 3,).map { "test-$it" }
    val items2: List<String> = listOf(1, 2, 3).parallelStream().map { "test-$it" }.toList()
    //endregion
    //region "async" functions
    TurboScope.execute {
        // 1) As a collection extension function within a scope.
        val list1 = listOf(1, 2 ,3).asyncMap {
            "test-$it"
        }
        // 2) Scope function, specifying collection as input.
        val list2 = asyncMap(input = listOf(1, 2 ,3)) {
            "test-$it"
        }
        // 3) Within a scope where its input is a collection.
        val list3: List<String> = job(input = listOf(1, 2 ,3)) {
            asyncMap { "test-$it" }
        }
    }
    TurboScope.execute {
        listOf(1, 2, 3, /*...*/).filter {
            // Kotlin filter identifies each item as "it".
            it < 10
        }
        listOf(1, 2, 3, /*...*/).asyncFilter {
            // io.turbodsl asyncFilter identifies each item as "input",
            // just like any other scope.
            // `it` can also be used - basically, `it` and `input` are synonyms.
            it < 10
        }
        asyncFilter(input = listOf(10, 15, 20, 25, 30, /*...*/)) {
            // "this" is a SyncScope, allowing to use other io.turbodsl expressions.
            // In this example, several asyncJobs are executed
            // filtering those items having more than 5 successes.
            async {
                repeat (it) {
                    asyncJob { /*...*/ }
                }
            }.let { r ->
                r.filter { it.isSuccess() }.size > 5
            }
        }
        val compensation: List<Compensation> = asyncFilter(input = job<List<Employee>> { emptyList<Employee>() /* returns a list of Employees (thousands) */ }) {
            // Each `input` is an Employee
            it.salary >= 3_000
        }.asyncMap {
            // Each `item` is an Employee with salary $3,000 or higher,
            // which is passed implicitly to each asyncJob through the `input` parameter.
            async(
                job1 = asyncJob<Benefit> { Benefit() /* retrieve benefits for employee */ },
                job2 = asyncJob<Timesheet> { Timesheet() /* retrieve timesheet for employee */ },
            ) { ok, r1, r2 ->
                if (ok) {
                    Compensation.Eligible(it, r1.success(), r2.success())
                } else {
                    Compensation.NonEligible(it)
                }
            }
        }
    }
    //endregion
    //region Boolean async Functions
    TurboScope.execute {
        // Forcing to run sequentially for this example
        listOf(1,2,3).asyncAll(strict = false, parallel = false) {
            if (it == 2) throw Error()
            it == 0
        }
    }
    // returns false, since the first item evaluates to false.

    TurboScope.execute {
        listOf(1,2,3).asyncAll(strict = true, parallel = false) {
            if (it == 2) throw Error()
            it == 0
        }
    }
    // throws ScopeExecutionException since evaluates all other items and fails on item `2`.
    //endregion
}
//endregion
