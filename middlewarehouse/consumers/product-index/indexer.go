package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/consumers"
	searchrow "github.com/FoxComm/highlander/middlewarehouse/consumers/product-index/search-row"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
)

const productCreatedActivity = "full_product_created"
const productUpdatedActivity = "full_product_updated"

type Indexer struct {
	esURL          string
	visualVariants []string
}

func NewIndexer(esURL string, esIndex string, esMapping string, visualVariants []string) (*Indexer, error) {
	if esURL == "" {
		return nil, errors.New("Indexer requires an ElasticSearch URL")
	} else if esIndex == "" {
		return nil, errors.New("Indexer requires an index in ElasticSearch")
	} else if esMapping == "" {
		return nil, errors.New("Indexer requires a mapping in ElasticSearch")
	}

	return &Indexer{fmt.Sprintf("%s/%s/%s", esURL, esIndex, esMapping), visualVariants}, nil
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

		id := fmt.Sprintf("%d-%d", prod.Product.ID, row.SKUs[0].ID)
		url := fmt.Sprintf("%s/%s", i.esURL, id)

		headers := map[string]string{}
		_, err = consumers.Put(url, headers, row)
		if err != nil {
			log.Printf("Unable to update product_catalog_view with error: %s", err.Error())
			return err
		}

		log.Printf("Updated view successfully")
	}

	return nil
}
