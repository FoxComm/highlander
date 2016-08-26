package services

import (
	"github.com/FoxComm/metamorphosis"
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

func LogActivity(producer metamorphosis.Producer, activity models.SiteActivity) error {
	rec, err := newRecord(activity)
	if err != nil {
		return nil, err
	}

	return producer.Emit(topic, rec)
}

type record struct {
	schema *avro.Schema

	// The formatting here is unfortunate, but required by how Avro handles parses.
	Id            int32
	Activity_type string
	Data          string
	Created_at    string
}

func newRecord(activity models.SiteActivity) (*record, error) {
	schema, err := avro.Parse(activityAvroSchema)
	if err != nil {
		return nil, err
	}

	return &record{
		schema:        schema,
		Activity_type: activity.Type(),
		Data:          activity.Data(),
		Created_at:    activity.CreatedAt(),
	}
}

func (a *record) Schema() avro.Schema {
	return a.schema
}
