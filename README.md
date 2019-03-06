# KafkaToAkka

This is a Demo project which purpose is to demonstrate Akka Actor connection with Kafka as Client and implementing back pressure while writing output to a file

## Code description

1. Main class creates a runnable graph of Akka actors
2. Actors are reading from Kafka Topic as a Soruce
3. Actors are broadcasting messages (Json snippets) read from Kafka to two different sinks (slowSink and bufferedSink)
4. Each Sink is implementing different backpressure mechanism
5. Sink outputs are written to files
