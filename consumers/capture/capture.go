package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/middlewarehouse/consumers"
	"github.com/FoxComm/middlewarehouse/consumers/capture/lib"
	"github.com/FoxComm/middlewarehouse/models/activities"
)

func CapturePayment(act activities.ISiteActivity, phoenixURL string, phoenixJWT string) error {
	capture, err := lib.NewCapturePayload(act)
	if err != nil {
		return err
	}

	url := fmt.Sprintf("%s/v1/service/capture", phoenixURL)
	headers := map[string]string{
		"JWT": phoenixJWT,
	}

	_, err = consumers.Post(url, headers, &capture)
	if err != nil {
		log.Printf(err.Error())
		return err
	}

	return nil
}
