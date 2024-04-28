# TurboDSL in a Kotlin application

This project contains samples on how to use `io.turbodsl` on standard applications.

A sample application is implemented using pure Kotlin and `TurboDSL`:
- Execute 3 jobs (job1, job2, job3) in parallel
- Execute a final job (job4) that will process the results of all previous 3

You can compare:
- Code structure
- Runtime between implementations
- How you can migrate to `TurboDSL`

See:
- [Pure Kotlin](src/main/kotlin/MainKotlin.kt) approach
- [TurboDSL](src/main/kotlin/MainTurboDSL.kt) approach

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
