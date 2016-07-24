package main

import (
	"fmt"
	"os"
	"os/signal"

	goavro "github.com/elodina/go-avro"
	"github.com/elodina/go-kafka-avro"
	"github.com/elodina/go_kafka_client"
)

func main() {
	zookeeper := "localhost:2181"
	schemaRepo := "http://localhost:8081"

	//Coordinator settings
	zookeeperConfig := go_kafka_client.NewZookeeperConfig()
	zookeeperConfig.ZookeeperConnect = []string{zookeeper}

	//Actual consumer settings
	consumerConfig := go_kafka_client.DefaultConsumerConfig()
	consumerConfig.AutoOffsetReset = go_kafka_client.SmallestOffset
	consumerConfig.Coordinator = go_kafka_client.NewZookeeperCoordinator(zookeeperConfig)
	consumerConfig.NumWorkers = 1
	consumerConfig.NumConsumerFetchers = 1
	consumerConfig.KeyDecoder = avro.NewKafkaAvroDecoder(schemaRepo)
	consumerConfig.ValueDecoder = consumerConfig.KeyDecoder

	consumerConfig.Strategy = func(worker *go_kafka_client.Worker, message *go_kafka_client.Message, taskId go_kafka_client.TaskId) go_kafka_client.WorkerResult {
		// time.Sleep(2 * time.Second)
		record, ok := message.DecodedValue.(*goavro.GenericRecord)
		if !ok {
			panic("Not a *GenericError, but expected one")
		}

		fmt.Println("SUCCESS")
		fmt.Printf("%v\n", record)

		return go_kafka_client.NewSuccessfulResult(taskId)
	}

	consumerConfig.WorkerFailureCallback = func(_ *go_kafka_client.WorkerManager) go_kafka_client.FailedDecision {
		return go_kafka_client.CommitOffsetAndContinue
	}
	consumerConfig.WorkerFailedAttemptCallback = func(_ *go_kafka_client.Task, _ go_kafka_client.WorkerResult) go_kafka_client.FailedDecision {
		return go_kafka_client.CommitOffsetAndContinue
	}

	kafkaConsumer := go_kafka_client.NewConsumer(consumerConfig)
	pingPongLoop(kafkaConsumer)
}

func pingPongLoop(kafkaConsumer *go_kafka_client.Consumer) {
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)
	go func() {
		<-c
		fmt.Println("\ngolang > Closing consumer")
		kafkaConsumer.Close()
	}()

	fmt.Println("golang > Started!")
	kafkaConsumer.StartStatic(map[string]int{
		"orders_search_view": 1,
	})
}
