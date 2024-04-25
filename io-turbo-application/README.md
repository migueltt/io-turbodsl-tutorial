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
> A _DSL engine_ to **turbo-charge** your Kotlin applications.

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
