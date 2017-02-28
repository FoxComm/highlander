package main

import (
	"encoding/base64"
	"errors"
	"log"
	"os/exec"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
)

type EventConsumer struct {
	ApiUrl string
}

func NewEventConsumer(apiUrl string) (*EventConsumer, error) {
	if apiUrl == "" {
		return nil, errors.New("API_URL is required")
	}

	return &EventConsumer{apiUrl}, nil
}

func (c EventConsumer) processActivity(activity activities.ISiteActivity) error {
	log.Printf("Processing %s: %s", activity.Type(), activity.Data())

	scriptText := "console.log('ACTIVITY: ' + type); api.get('/v1/public/products/' + data.cart.lineItems.skus[0].productFormId).then(function (resp) { console.log('RESPONSE: ');console.log(resp);});"

	data := base64.StdEncoding.EncodeToString([]byte(activity.Data()))
	script := base64.StdEncoding.EncodeToString([]byte(scriptText))

	//Get from DB
	jwt := "test-jwt"
	stripeKey := "test-stripe"

	out, err := exec.Command("node", "--harmony", "executor/main.js", c.ApiUrl, stripeKey, jwt, activity.Type(), data, script).CombinedOutput()

	log.Printf("OUT: %s\n", out)
	log.Printf("ERR: %v\n", err)
	return nil
}

func (c EventConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Fatalf("Unable to read activity with error %s", err.Error())
		return err
	}

	log.Printf("Activity: %s", activity.Type())
	err = c.processActivity(activity)
	if err != nil {
		log.Fatalf("Unable to parse json with error %s", err.Error())
	}
	return err
}
