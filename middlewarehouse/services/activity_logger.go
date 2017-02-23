package services

import (
	"log"
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
	avro "github.com/elodina/go-avro"
)

var avroSchema avro.Schema

func init() {
	var err error
	avroSchema, err = avro.ParseSchema(activityAvroSchema)
	if err != nil {
		log.Fatalf("Unable to parse Avro schema for activities")
	}
}

const (
	topic              = "activities"
	activityAvroSchema = `{
			"type": "record",
			"name": "activities",
			"namespace": "com.martinkl.bottledwater.dbschema.public",
			"fields": [
					{
							"name": "id",
							"type": ["null", "int"]
					},
					{
							"name": "activity_type",
							"type": ["null", "string"]
					},
					{
							"name": "data",
							"type": ["null", "string"]
					},
					{
							"name": "context",
							"type": ["null", "string"]
					},
					{
							"name": "created_at",
							"type": ["null", "string"]
					},
					{
                                                        "name": "scope",
                                                         "type": ["null","string"]
                                        }
			]
	}`
)

// IActivityLogger is the service responsible for saving activities that are
// part of the activity trail to Kafka.
type IActivityLogger interface {
	Log(activity activities.ISiteActivity) error
}

// NewActivityLogger creates a new instance on an activity logger with the
// default configuration.
func NewActivityLogger(producer metamorphosis.Producer) IActivityLogger {
	return &activityLogger{producer}
}

type activityLogger struct {
	producer metamorphosis.Producer
}

func (a *activityLogger) Log(activity activities.ISiteActivity) error {
	rec, err := newRecord(activity)
	if err != nil {
		return err
	}

	return a.producer.Emit(topic, rec)
}

type record struct {
	schema avro.Schema

	// The formatting here is unfortunate, but required by how Avro handles parses.
	Id            int32
	Activity_type string
	Data          string
	Created_at    string
	Context       string
	Scope	      string
}

func newRecord(activity activities.ISiteActivity) (*record, error) {
	const contextTemplate = "{\"userId\":0,\"userType\":\"service\", \"transactionId\":\"mwh\", \"scope\":\"%v\"}"
	var context =  fmt.Sprintf(contextTemplate, activity.Scope())

	return &record{
		schema:        avroSchema,
		Id:            1,
		Activity_type: activity.Type(),
		Data:          activity.Data(),
		Created_at:    activity.CreatedAt(),
		Context:       context,
		Scope:         activity.Scope(),
	}, nil
}

func (a *record) Schema() avro.Schema {
	return a.schema
}
