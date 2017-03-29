package main

import (
	"errors"
	"fmt"
	"gopkg.in/olivere/elastic.v3"
	"log"
	"time"
)

type ProductActivityMonitor struct {
	hhClient      *Henhouse
	esClient      *elastic.Client
	interval      int64
	lastQueryTime time.Time
	lastValue     int64
	key           string
	index         string
}

func NewProductActivityMonitor(henhouse *Henhouse, esClient *elastic.Client, index string, interval int) (*ProductActivityMonitor, error) {
	if henhouse == nil {
		return nil, errors.New("henhouse is required")
	}

	if interval <= 1 {
		return nil, fmt.Errorf("Invalid interval value %d. Should be at least 1 second", interval)
	}

	return &ProductActivityMonitor{henhouse, esClient, int64(interval), time.Now(), 0, "track.1.products.activated", index}, nil
}

const queryPattern = `
{"query": {
    "constant_score": {
      "filter": {
        "bool": {
          "must": [
            {"exists": { "field": "activeFrom"} },
            {"range": {
                "activeFrom": {
                  "lte": "%s",
                  "time_zone": "+00:00"
                }
              }
            },
            {"bool": {
            	"should": [
            		{"bool": {"must_not":{"exists": {"field": "archivedAt"} } } },
            		{"range": {
                		"archivedAt": {
                			"gt": "%s",
                			"time_zone": "+00:00"
                			}
            			}
            		}
            		]
            } }
          ],
          "should": [
          	{"bool": {"must_not":{"exists": {"field": "activeTo"} } } },
            {"range": {
                "activeTo": {
                  "gt": "%s",
                  "time_zone": "+00:00"
                }
              }
            }
          ]
        }
      }
    }
  }
}
`

func (o *ProductActivityMonitor) start() error {
	err := o.queryFirstTime()
	if err != nil {
		return err
	}

	for {
		wait := o.interval - (time.Now().Unix() - o.lastQueryTime.Unix())

		if wait > 0 {
			timer := time.NewTimer(time.Duration(wait) * time.Second)
			<-timer.C
		}
		err := o.perform()
		if err != nil {
			return err
		}
	}
}

func (o *ProductActivityMonitor) queryFirstTime() error {
	log.Print("Querying henhouse for first time")

	values, err := o.hhClient.Summary([]string{o.key})
	if err != nil {
		return err
	}

	if values[0].Stats.To == 0 {
		o.lastQueryTime = time.Now().Add(time.Duration(-o.interval) * time.Second)
		o.lastValue = int64(0)
	} else {
		o.lastQueryTime = time.Unix(values[0].Stats.To, 0)
		o.lastValue = values[0].Stats.Sum
	}
	return nil
}

func (o *ProductActivityMonitor) perform() error {
	if ((o.lastQueryTime).Unix() + o.interval) < time.Now().Unix() {

		end := time.Unix(o.lastQueryTime.Unix()+o.interval, 0)
		errorContext:= fmt.Sprintf("%s - %s", o.lastQueryTime.Format(time.RFC3339), end.Format(time.RFC3339))

		value, err := o.queryES(o.lastQueryTime, end)
		if err != nil {
			log.Fatalf("%s: cannot query ES: %s", errorContext, err.Error())
			return err
		}
		err = o.track(value-o.lastValue, end)
		if err != nil {
			log.Fatalf("%s: cannot track value: %s", errorContext, err.Error())
			return err
		}
		log.Printf("%s ES value %d, Henhouse value %d", errorContext, value, value-o.lastValue)

		o.lastQueryTime = end
		o.lastValue = value
	}
	return nil
}

func (o *ProductActivityMonitor) queryES(from time.Time, to time.Time) (int64, error) {
	query := fmt.Sprintf(queryPattern, to.Format(time.RFC3339), from.Format(time.RFC3339), from.Format(time.RFC3339))
	q := elastic.RawStringQuery(query)
	return o.esClient.Count(o.index).Type("products_search_view").Query(q).Do()
}

func (o *ProductActivityMonitor) track(quantity int64, time time.Time) error {
	return o.hhClient.Track(o.key, quantity, time)
}
