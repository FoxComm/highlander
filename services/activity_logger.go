package services

import (
	"github.com/FoxComm/metamorphosis"
	"github.com/FoxComm/middlewarehouse/models/activities"
	avro "github.com/elodina/go-avro"
)

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
					}
			]
	}`
)

// IActivityLogger is the service responsible for saving activities that are
// part of the activity trail to Kafka.
type IActivityLogger interface {
	Log(activity activities.SiteActivity) error
}

// NewActivityLogger creates a new instance on an activity logger with the
// default configuration.
func NewActivityLogger(producer metamorphosis.Producer) IActivityLogger {
	return &activityLogger{producer}
}

type activityLogger struct {
	producer metamorphosis.Producer
}

func (a *activityLogger) Log(activity activities.SiteActivity) error {
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
}

func newRecord(activity activities.SiteActivity) (*record, error) {
	schema, err := avro.ParseSchema(activityAvroSchema)
	if err != nil {
		return nil, err
	}

	return &record{
		schema:        schema,
		Id:            1,
		Activity_type: activity.Type(),
		Data:          activity.Data(),
		Created_at:    activity.CreatedAt(),
		Context:       "",
	}, nil
}

func (a *record) Schema() avro.Schema {
	return a.schema
}
