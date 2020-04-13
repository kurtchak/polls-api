package sk.blackbell.pollsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PollsApiApplication

fun main(args: Array<String>) {
	runApplication<PollsApiApplication>(*args)
}
