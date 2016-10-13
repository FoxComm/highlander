package metamorphosis

import (
	"os"
	"os/signal"

	"github.com/elodina/go-kafka-avro"
	"github.com/elodina/go_kafka_client"
)

// Consumer represents the interface for consuming data from a Kafka topic.
type Consumer interface {
	// RunTopic runs a message handler against a topic and partition. The handler
	// gets called each time a new message is received.
	RunTopic(topic string, partition int, handler Handler)
}

type consumer struct {
	config *go_kafka_client.ConsumerConfig
}

func NewConsumer(zookeeper string, schemaRepo string) (Consumer, error) {
	zConfig := go_kafka_client.NewZookeeperConfig()
	zConfig.ZookeeperConnect = []string{zookeeper}

	consumerConfig := go_kafka_client.DefaultConsumerConfig()
	consumerConfig.AutoOffsetReset = go_kafka_client.SmallestOffset
	consumerConfig.Coordinator = go_kafka_client.NewZookeeperCoordinator(zConfig)
	consumerConfig.NumWorkers = 1
	consumerConfig.NumConsumerFetchers = 1
	consumerConfig.KeyDecoder = avro.NewKafkaAvroDecoder(schemaRepo)
	consumerConfig.ValueDecoder = consumerConfig.KeyDecoder

	consumerConfig.WorkerFailureCallback = defaultFailureCallback
	consumerConfig.WorkerFailedAttemptCallback = defaultFailedAttemptCallback

	return &consumer{config: consumerConfig}, nil
}

func (c consumer) RunTopic(topic string, partition int, handler Handler) {
	c.config.Strategy = createStrategy(handler)

	kafkaConsumer := go_kafka_client.NewConsumer(c.config)

	ch := make(chan os.Signal, 1)
	signal.Notify(ch, os.Interrupt)

	go func() {
		<-ch
		kafkaConsumer.Close()
	}()

	kafkaConsumer.StartStatic(map[string]int{topic: partition})
}

func createStrategy(fn Handler) go_kafka_client.WorkerStrategy {
	return func(
		worker *go_kafka_client.Worker,
		message *go_kafka_client.Message,
		taskId go_kafka_client.TaskId) go_kafka_client.WorkerResult {

		record, err := newAvroMessage(message)
		if err != nil {
			panic(err)
		}

		if err := fn(record); err != nil {
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
