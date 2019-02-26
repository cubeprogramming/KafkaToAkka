package no.sysco

//import java.io.File
import java.time.Duration
import java.util.{Collections, Properties}

import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecords, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer

import scala.collection.JavaConverters._

//#main-class
object SimpleKafkaReader extends App {
  val bootstrapServers = "127.0.0.1:9092"
  val groupId = "kafka-to-akka"
  val topic = "csv_lines"


  // create consumer configs
  val properties = new Properties
  properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
  properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId)
  properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  // create consumer
  val consumer = new KafkaConsumer[String,String](properties)

  // subscribe consumer to our topic
  consumer.subscribe(Collections.singleton(topic))

  //poll for new data
  while (true) {
    val consumerRecords: ConsumerRecords[String,String] = consumer.poll(Duration.ofMillis(100))

    val output1 = consumerRecords.asScala.map(record => s"Key: ${record.key()}, Value: ${record.value()} ")

    /*val output2 = for {
      record: ConsumerRecord[String,String] <- consumerRecords.asScala
    } yield s"Key: ${record.key()}, Value: ${record.value()} "*/

    println(output1)

    /*for {
      record: ConsumerRecord[String,String] <- consumerRecords.asScala
    } println( s"Key: ${record.key()}, Value: ${record.value()} ")*/
  }


}
//#main-class