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

import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

/** Just to standardize output. */
fun Any.printTest(msg: String) {
    // Comment this line to remove any output
    println("${LocalDateTime.now()} - ${Thread.currentThread()} - $this -> $msg")
}

/** Just to simplify iterations and measurement. */
fun measure(
    times: Int = 1,
    block: () -> Unit,
) {
    repeat(times) {
        val iteration = it + 1
        println("Iteration: $iteration -> ${measureTimeMillis(block)}ms")
    }
}
