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
> A _DSL-engine_ to **turbo-charge** your Kotlin development.

## Objectives
> `TurboDSL` will not make your application faster, but will make your development easier,
> and most importantly, write asynchronous code in a natural way.

## Fundamentals
- Everything is based on DSL expressions using [Kotlin approach](https://kotlinlang.org/docs/type-safe-builders.html)
- _Scopes_ are created internally to maintain structure and call hierarchy
- Each _scope_ allows additional DSL expressions, making `TurboDSL` usage more idiomatic
- The runtime overhead is minimal, comparing to pure Kotlin coroutines implementation.

## Features
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

## Tutorials
- Using on a Kotlin application - [io-turbo-application](io-turbo-application)
- Using on an Android application - coming soon!
- Using on a back-end application - coming soon!
