import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

fun Any.printTest(msg: String) {
    println("${LocalDateTime.now()} - ${Thread.currentThread()} - $this -> $msg")
}

fun measure(
    times: Int = 1,
    block: () -> Unit
) {
    repeat(times) {
        val iteration = it + 1
        println("Iteration: $iteration -> ${measureTimeMillis(block)}ms")
    }
}
