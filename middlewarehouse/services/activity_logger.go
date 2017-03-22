package services

import (
	"fmt"
	"log"

	"errors"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
	avro "github.com/elodina/go-avro"
	"github.com/jinzhu/gorm"
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
	topic              = "scoped_activities"
	activityAvroSchema = `{
			"type": "record",
			"name": "scoped_activities",
			"fields": [
					{
							"name": "id",
							"type": ["null", "string"]
					},
					{
							"name": "kind",
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
func NewActivityLogger(producer metamorphosis.Producer, db *gorm.DB) IActivityLogger {
	return &activityLogger{producer, db}
}

type activityLogger struct {
	producer metamorphosis.Producer
	db       *gorm.DB
}

func (a *activityLogger) Log(activity activities.ISiteActivity) error {
	rec, err := newRecord(activity)

	if err != nil {
		return err
	}

	rows, err := a.db.Raw("select nextval('activities_id_seq');").Rows()

	if err != nil {
		return err
	}

	defer rows.Close()

	var nextId int32

	if rows.Next() {
		rows.Scan(&nextId)
		rec.Id = fmt.Sprintf("%v-%v", "mwh", nextId)

		return a.producer.Emit(topic, rec)
	} else {
		return errors.New("Unable get activity id")
	}

}

type record struct {
	schema avro.Schema

	// The formatting here is unfortunate, but required by how Avro handles parses.
	Id         string
	Kind       string
	Data       string
	Created_at string
	Context    string
	Scope      string
}

func newRecord(activity activities.ISiteActivity) (*record, error) {
	const contextTemplate = "{\"userId\":0,\"userType\":\"service\", \"transactionId\":\"mwh\", \"scope\":\"%v\"}"
	var context = fmt.Sprintf(contextTemplate, activity.Scope())

	return &record{
		schema:     avroSchema,
		Id:         "",
		Kind:       activity.Type(),
		Data:       activity.Data(),
		Created_at: activity.CreatedAt(),
		Context:    context,
		Scope:      activity.Scope(),
	}, nil
}

func (a *record) Schema() avro.Schema {
	return a.schema
}
