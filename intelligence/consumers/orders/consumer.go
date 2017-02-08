package main

import (
	"crypto/sha1"
	"encoding/json"
	"errors"
	"fmt"
	"log"
	"net"
	"strconv"
	"time"

	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/metamorphosis"
)

type Sku struct {
	ProductId int `json:"productFormId"`
}

type LineItems struct {
	Skus []Sku `json:"skus"`
}

type Customer struct {
	Id int `json:"id"`
}

type Order struct {
	Customer  Customer  `json:"customer"`
	LineItems LineItems `json:"lineItems"`
}

type Activity struct {
	Order Order `json:"order"`
}

type OrderConsumer struct {
	henhouseHost string
	henhouseConn net.Conn
}

func NewOrderConsumer(henhouseHost string) (*OrderConsumer, error) {
	if henhouseHost == "" {
		return nil, errors.New("henhouse host is required")
	}

	conn, err := net.Dial("tcp", henhouseHost+":2003")
	if err != nil {
		return nil, fmt.Errorf("Unable to connect to henhouse with error %s", err.Error())
	}

	return &OrderConsumer{henhouseHost, conn}, nil
}

func (o OrderConsumer) parseData(data string) error {
	act := Activity{}
	jsonErr := json.Unmarshal([]byte(data), &act)
	if jsonErr != nil {
		return jsonErr
	}

	skus := act.Order.LineItems.Skus
	for i := 0; i < len(skus); i++ {
		err := o.track(act.Order.Customer.Id, skus[i].ProductId)
		if err != nil {
			return err
		}
	}

	return nil
}

func (o OrderConsumer) track(custId, prodId int) error {
	hash := sha1.Sum([]byte("products/" + strconv.Itoa(prodId)))
	datetime := time.Now().Unix()
	channel := 1
	count := 1

	fmt.Fprintf(o.henhouseConn, "track.%d.product.%x.purchase.%d %d %s\n", channel, hash, custId, count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%d.product.%x.purchase %d %s\n", channel, hash, count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%d.product.purchase %d %s\n", channel, count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.product.%x.purchase.%d %d %s\n", hash, custId, count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.product.%x.purchase %d %s\n", hash, count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.product.purchase %d %s\n", count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.purchase.%d %d %s\n", custId, count, datetime)
	fmt.Fprintf(o.henhouseConn, "track.purchase %d %s\n", count, datetime)

	return nil
}

func (o OrderConsumer) Handler(message metamorphosis.AvroMessage) error {
	activity, err := activities.NewActivityFromAvro(message)
	if err != nil {
		log.Fatalf("Unable to read activity with error %s", err.Error())
		return err
	}

	if activity.Type() == orderCheckoutCompleted {
		err := o.parseData(activity.Data())
		if err != nil {
			log.Fatalf("Unable to parse json with error %s", err.Error())
		}
		return err
	}
	return nil
}
