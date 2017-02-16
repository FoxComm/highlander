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
	Quantity  int `json:"quantity"`
	Price     int `json:"price"`
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
	RefNum    string    `json:"referenceNumber"`
}

type Activity struct {
	Order Order `json:"order"`
}

type OrderConsumer struct {
	henhouseConn net.Conn
}

const (
	orderCheckoutCompleted = "order_checkout_completed"
)

func NewOrderConsumer(henhouseHost string) (*OrderConsumer, error) {
	if henhouseHost == "" {
		return nil, errors.New("henhouse host is required")
	}

	henhouseConn, err := net.Dial("tcp", henhouseHost+":2003")
	if err != nil {
		return nil, fmt.Errorf("Unable to connect to henhouse with error %s", err.Error())
	}

	return &OrderConsumer{henhouseConn}, nil
}

func (o OrderConsumer) productHash(prodId int) [20]byte {
	return sha1.Sum([]byte("products/" + strconv.Itoa(prodId)))
}

func (o OrderConsumer) orderHash(refnum string) [20]byte {
	return sha1.Sum([]byte("orders/" + refnum))
}

func (o OrderConsumer) parseData(data string) error {
	act := Activity{}
	jsonErr := json.Unmarshal([]byte(data), &act)
	if jsonErr != nil {
		return jsonErr
	}

	err := o.track(o.orderHash(act.Order.RefNum), "order", "puchase", act.Order.Customer.Id, 1)
	if err != nil {
		return err
	}

	skus := act.Order.LineItems.Skus
	for i := 0; i < len(skus); i++ {
		err1 := o.track(o.productHash(skus[i].ProductId), "product", "purchase", act.Order.Customer.Id, skus[i].Quantity)
		if err1 != nil {
			return err1
		}
		err2 := o.track(o.productHash(skus[i].ProductId), "product", "revenue", act.Order.Customer.Id, skus[i].Price)
		if err2 != nil {
			return err2
		}
	}

	return nil
}

func (o OrderConsumer) track(hash [20]byte, object, verb string, custId, quantity int) error {
	datetime := time.Now().Unix()
	channel := 1

	fmt.Fprintf(o.henhouseConn, "track.%d.%s.%x.%s.%d %d %d\n", channel, object, hash, verb, custId, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%d.%s.%x.%s %d %d\n", channel, object, hash, verb, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%d.%s.%s %d %d\n", channel, object, verb, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%s.%x.%s.%d %d %d\n", object, hash, verb, custId, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%s.%x.%s %d %d\n", object, hash, verb, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%s.%s %d %d\n", object, verb, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%s.%d %d %d\n", verb, custId, quantity, datetime)
	fmt.Fprintf(o.henhouseConn, "track.%s %d %d\n", verb, quantity, datetime)

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
