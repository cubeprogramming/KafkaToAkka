package no.sysco

import java.time.Duration
import java.util.{Collections, Properties}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.stream.{ActorMaterializer, IOResult}
import better.files.File
//import better.files.Scanner.Source
import akka.kafka._
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.serialization.StringDeserializer

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.concurrent.duration._

//#main-class
object KafkaReader extends App {
  val bootstrapServers = "127.0.0.1:9092"
  val groupId = "kafka-to-akka"
  val topic = "csv_lines"

  // create Actor system
  implicit val system = ActorSystem("kafka-consumer")
  implicit val materializer = ActorMaterializer()

  val sink1 = lineSink("resources/output1.txt")
  val sink2 = lineSink("resources/output2.txt")
  val slowSink2 = Flow[String].via(Flow[String].throttle(1, 1.second, 1, ThrottleMode.shaping)).toMat(sink2)(Keep.right)
  val bufferedSink2 = Flow[String].buffer(50, OverflowStrategy.backpressure).via(Flow[String].throttle(1, 1.second, 1, ThrottleMode.shaping)).toMat(sink2)(Keep.right)


  // create consumer configs
  val config = system.settings.config.getConfig("akka.kafka.consumer")
  val consumerSettings =
    ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(bootstrapServers)
      .withGroupId(groupId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")



  val done = Consumer.committablePartitionedSource(consumerSettings, Subscriptions.topics(topic))
    .flatMapMerge(maxPartitions, _._2)
    .via(business)
    .batch(max = 20, first => CommittableOffsetBatch.empty.updated(first.committableOffset)) { (batch, elem) =>
      batch.updated(elem.committableOffset)
    }
    .mapAsync(3)(_.commitScaladsl())
    .runWith(Sink.ignore)


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

  val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val bcast = b.add(Broadcast[String](2))
    val consumerRecords: ConsumerRecords[String,String] = consumer.poll(Duration.ofMillis(100))
    val jsonRec: Source[String, NotUsed] = Source(consumerRecords.asScala.map(record => record.value()).toStream)
    jsonRec ~> bcast.in
    bcast.out(0) ~> sink1
    bcast.out(1) ~> bufferedSink2
    ClosedShape
  })

  g.run()


  def lineSink(filename: String): Sink[String, Future[IOResult]] = {
    Flow[String]
      .alsoTo(Sink.foreach(s => println(s"$filename: $s")))
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toPath(File(filename).path))(Keep.right)
  }
}
//#main-class