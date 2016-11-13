package main

import (
	"fmt"
	"log"

	"github.com/FoxComm/highlander/consumers/product-index/search"
	"github.com/FoxComm/highlander/shared/golang/activities"
	"github.com/FoxComm/highlander/shared/golang/elastic"
)

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

func (i Indexer) Run(activity activities.SiteActivity) error {
	switch activity.Type() {
	case activities.ProductCreated:
	case activities.ProductUpdated:
		fullProduct := activity.(activities.FullProduct)
		return i.indexProductActivity(fullProduct)
	}

	return nil
}

func (i Indexer) indexProductActivity(activity activities.FullProduct) error {
	product := activity.Product()

	existingRows, err := i.existingSearchRows(product.ID)
	if err != nil {
		return err
	}

	partialProducts, err := search.MakePartialProducts(product, i.visualVariants)
	if err != nil {
		return fmt.Errorf("Error creating partial products with error: %s", err.Error())
	}

	for _, p := range partialProducts {
		row, err := search.NewSearchRow(product, p)
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
