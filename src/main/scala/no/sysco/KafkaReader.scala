package no.sysco

import java.nio.file.{FileSystems, Files}
import java.time.Instant
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer.committableSource
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerMessage, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{FileIO, Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContextExecutor, Future}

//#main-class
object KafkaReader extends App {
  val appConfig: ApplicationConfig = ApplicationConfig(ConfigFactory.load())
  val bootstrapServers = appConfig.Kafka.bootstrapServers
  val topic = appConfig.Kafka.topic

  // create Actor system
  implicit val system = ActorSystem("kafka-consumer")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = materializer.executionContext

  val fs = FileSystems.getDefault

  println(system.settings.config.getConfig("akka.kafka.consumer"))

  val consumerSettings: ConsumerSettings[String, String] = {
    ConsumerSettings(
      system,
      new StringDeserializer,
      new StringDeserializer
    )
      .withBootstrapServers(appConfig.Kafka.bootstrapServers)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withGroupId("kafka-to-akka-group-1")
      .withClientId("kafka-to-akka-id-1")
  }

  val committerSettings = CommitterSettings(system)
  val committerSink: Sink[ConsumerMessage.Committable, Future[Done]] = Committer.sink(committerSettings)

  val kafkaSourceCommittable: Source[ConsumerMessage.CommittableMessage[String, String], Consumer.Control] =
    committableSource(consumerSettings, Subscriptions.topics(topic))

  val flowFile: Flow[ConsumerMessage.CommittableMessage[String, String], ConsumerMessage.CommittableOffset, NotUsed] = Flow[ConsumerMessage.CommittableMessage[String, String]]
    .map(msg => {
      val path = Files.createTempFile(fs.getPath("outputs"), s"${msg.record.key}-${Instant.now.toEpochMilli}", ".log")
      Source.single(ByteString(msg.record.value())).runWith(FileIO.toPath(path)).onComplete(done => {
        if(done.isSuccess) {
          println(s"Commit msg with key: ${msg.record.key()} \n and value: ${msg.record.value()}")
          msg.committableOffset.commitScaladsl()
        }
        else {
          println(s"roll back || do not commit || stop stream || FAILED with msg : ${msg.record.key()}")
          system.terminate()
          //msg.committableOffset.commitScaladsl()
        }
      })
      msg.committableOffset
    })
    .buffer(20, OverflowStrategy.backpressure)
    .throttle(10, Duration.create(5, TimeUnit.SECONDS), 5, ThrottleMode.shaping)


  kafkaSourceCommittable.via(flowFile).runWith(Sink.ignore)

}
//#main-class