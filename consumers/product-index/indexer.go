package main

import (
	"encoding/json"
	"fmt"
	"log"

	searchrow "github.com/FoxComm/highlander/consumers/product-index/search-row"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/shared/golang/elastic"
)

const productCreatedActivity = "full_product_created"
const productUpdatedActivity = "full_product_updated"

type Indexer struct {
	esClient       *elastic.Client
	visualVariants []string
}

func NewIndexer(esURL string, esIndex string, esMapping string, visualVariants []string) (*Indexer, error) {
	esClient, err := elastic.NewClient(esURL, esIndex, esMapping)
	if err != nil {
		return nil, err
	}

	return &Indexer{esClient, visualVariants}, nil
}

func (i Indexer) Run(activity activities.ISiteActivity) error {
	if activity.Type() != productCreatedActivity && activity.Type() != productUpdatedActivity {
		return nil
	}

	log.Printf("Processing: %s", activity.Data())

	bt := []byte(activity.Data())
	prod := new(ConsumerProduct)

	if err := json.Unmarshal(bt, prod); err != nil {
		return fmt.Errorf("Error unmarshalling activity data into product with error: %s", err.Error())
	}

	partialProducts, err := searchrow.MakePartialProducts(prod.Product, i.visualVariants)
	if err != nil {
		return fmt.Errorf("Error creating partial products with error: %s", err.Error())
	}

	for _, p := range partialProducts {
		row, err := searchrow.NewSearchRow(prod.Product, p)
		if err != nil {
			log.Printf("Unable to create search row with error: %s", err.Error())
			return err
		}

		if err := i.esClient.UpdateDocument(row.Identifier(), row); err != nil {
			log.Printf("Unable to update document with error: %s", err.Error())
			return err
		}

		log.Printf("Updated view successfully")
	}

	return nil
}
