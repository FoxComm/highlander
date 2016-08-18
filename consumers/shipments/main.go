package shipments

import (
	"log"
	"os"
	"strconv"

	"github.com/FoxComm/metamorphosis"
)

func main() {
	zookeeper := os.Getenv("ZOOKEEPER")
	schemaRegistry := os.Getenv("SCHEMA_REGISTRY")

	consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRegistry)
	if err != nil {
		log.Fatalf("Unable to connect to Kafka with error %s", err.Error())
	}

	topic := os.Getenv("TOPIC")
	partition := os.Getenv("PARTITION")
	partNum, err := strconv.Atoi(partition)
	if err != nil {
		log.Fatalf("Unable to get Kafka partition with error %s", err.Error())
	}

	consumer.RunTopic(topic, partNum, FulfilledOrderHandler)
}
