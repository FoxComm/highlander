package main

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/consumers/product-index/search"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/shared/golang/elastic"
)

const productCreatedActivity = "full_product_created"
const productUpdatedActivity = "full_product_updated"

type Indexer struct {
	esClient       *elastic.Client
	IndexerConfig
}

func NewIndexer(esURL string, esIndex string, esMapping string, idxConfig *IndexerConfig) (*Indexer, error) {
	esClient, err := elastic.NewClient(esURL, esIndex, esMapping)
	if err != nil {
		return nil, err
	}

	return &Indexer{esClient, *idxConfig}, nil
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

	existingRows, err := i.existingSearchRows(prod.Product.ID)
	if err != nil {
		return err
	}

	partialProducts, err := search.MakePartialProducts(prod.Product, i.visualVariants)
	if err != nil {
		return fmt.Errorf("Error creating partial products with error: %s", err.Error())
	}

	for _, p := range partialProducts {
		row, err := search.NewSearchRow(prod.Product, p)
		if err != nil {
			log.Printf("Unable to create search row with error: %s", err.Error())
			return err
		}

		if err := i.esClient.UpdateDocument(row.Identifier(), row); err != nil {
			log.Printf("Unable to update document with error: %s", err.Error())
			return err
		}

		delete(existingRows, row.Identifier())
		log.Printf("Successfully updated view for _id %s", row.Identifier())
	}

	for id, _ := range existingRows {
		if err := i.esClient.RemoveDocument(id); err != nil {
			return err
		}
	}

	return nil
}

func (i Indexer) existingSearchRows(productID int) (map[string]search.SearchRow, error) {
	filters := []elastic.TermFilter{
		elastic.TermFilter{Field: "productId", Value: productID},
	}

	query, err := elastic.NewCompiledTermFilter(filters)
	if err != nil {
		return nil, err
	}

	result, err := i.esClient.ExecuteSearch(query)
	if err != nil {
		return nil, err
	}

	rows := map[string]search.SearchRow{}
	for _, hit := range result.ExtractHits() {
		row := search.SearchRow{}
		if err := hit.Extract(&row); err != nil {
			return nil, err
		}

		rows[hit.ID] = row
	}

	return rows, nil
}
