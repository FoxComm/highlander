package metamorphosis

import (
	"errors"
	"fmt"

	"github.com/elodina/go-avro"
	"github.com/elodina/go_kafka_client"
)

// AvroMessage represents a Kafka message that's been successfully decoded with
// Avro.
type AvroMessage interface {
	// Bytes returns a byte array containing the parsed contents of the message.
	Bytes() []byte
}

type avroMessage struct {
	record *avro.GenericRecord
}

func newAvroMessage(message *go_kafka_client.Message) (AvroMessage, error) {
	record, ok := message.DecodedValue.(*avro.GenericRecord)
	if !ok {
		return nil, errors.New("Unable to decode message")
	}

	return &avroMessage{record}, nil
}

func (am avroMessage) Bytes() []byte {
	recordStr := fmt.Sprintf("%v", am.record)
	return []byte(recordStr)
}
