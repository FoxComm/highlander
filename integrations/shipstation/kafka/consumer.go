package kafka

import (
	"fmt"
	"os"
	"os/signal"

	"github.com/elodina/go-kafka-avro"
	"github.com/elodina/go_kafka_client"
)

type MessageHandler func(message *go_kafka_client.Message) error

type Consumer interface {
	RunTopic(string)
}

type consumer struct {
	kafkaConsumer *go_kafka_client.Consumer
}

func NewConsumer(zookeeper string, schemaRepo string, handler MessageHandler) (Consumer, error) {
	zConfig := go_kafka_client.NewZookeeperConfig()
	zConfig.ZookeeperConnect = []string{zookeeper}

	//Actual consumer settings
	consumerConfig := go_kafka_client.DefaultConsumerConfig()
	consumerConfig.AutoOffsetReset = go_kafka_client.SmallestOffset
	consumerConfig.Coordinator = go_kafka_client.NewZookeeperCoordinator(zConfig)
	consumerConfig.NumWorkers = 1
	consumerConfig.NumConsumerFetchers = 1
	consumerConfig.KeyDecoder = avro.NewKafkaAvroDecoder(schemaRepo)
	consumerConfig.ValueDecoder = consumerConfig.KeyDecoder

	consumerConfig.Strategy = createDefaultStrategy(handler)

	consumerConfig.WorkerFailureCallback = defaultFailureCallback
	consumerConfig.WorkerFailedAttemptCallback = defaultFailedAttemptCallback

	kafkaConsumer := go_kafka_client.NewConsumer(consumerConfig)
	return &consumer{kafkaConsumer: kafkaConsumer}, nil
}

func (con *consumer) RunTopic(topic string) {
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)

	go func() {
		<-c
		fmt.Println("Closing ShipStation consumer")
		con.kafkaConsumer.Close()
	}()

	fmt.Println("ShipStation consumer started!")
	con.kafkaConsumer.StartStatic(map[string]int{
		topic: 1,
	})
}

func createDefaultStrategy(fn MessageHandler) go_kafka_client.WorkerStrategy {
	return func(
		worker *go_kafka_client.Worker,
		message *go_kafka_client.Message,
		taskId go_kafka_client.TaskId) go_kafka_client.WorkerResult {

		if err := fn(message); err != nil {
			panic(err)
		}

		return go_kafka_client.NewSuccessfulResult(taskId)
	}
}

func defaultFailureCallback(_ *go_kafka_client.WorkerManager) go_kafka_client.FailedDecision {
	return go_kafka_client.CommitOffsetAndContinue
}

func defaultFailedAttemptCallback(_ *go_kafka_client.Task, _ go_kafka_client.WorkerResult) go_kafka_client.FailedDecision {
	return go_kafka_client.CommitOffsetAndContinue
}
