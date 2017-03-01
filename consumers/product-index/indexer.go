package main

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/FoxComm/highlander/consumers/product-index/search"
	"github.com/FoxComm/highlander/middlewarehouse/models/activities"
	"github.com/FoxComm/highlander/shared/golang/elastic"

	consul "github.com/hashicorp/consul/api"
	"errors"
)

const productCreatedActivity = "full_product_created"
const productUpdatedActivity = "full_product_updated"
const productArchivedActivity = "full_product_archived"

type Indexer struct {
	esClient     *elastic.Client
	consulClient *consul.Client
	IndexerConfig
}

func (i Indexer) FetchSearchConfig() (*search.Config, error) {
	consulKv := i.consulClient.KV()
	pair, _, err := consulKv.Get("search/config/products_search", nil)
	if err != nil {
		return nil, err
	}
	if pair == nil {
		return nil, errors.New("Search settings is empty")
	}

	searchConfig := search.Config{}
	err = json.Unmarshal(pair.Value, &searchConfig)
	if err != nil {
		return nil, err
	}

	return &searchConfig, nil
}

func NewIndexer(esURL string, esIndex string, esMapping string, idxConfig *IndexerConfig) (*Indexer, error) {
	esClient, err := elastic.NewClient(esURL, esIndex, esMapping)
	if err != nil {
		return nil, err
	}

	consulCfg := &consul.Config{Address: idxConfig.consulAddress, Scheme: idxConfig.consulScheme}
	consulClient, err := consul.NewClient(consulCfg)
	if err != nil {
		return nil, err
	}

	return &Indexer{esClient, consulClient, *idxConfig}, nil
}

func (i Indexer) onArchiveProduct(prod *ConsumerProduct) error {
	existingRows, err := i.existingSearchRows(prod.Product.ID)
	if err != nil {
		return err
	}
	for id, _ := range existingRows {
		if err := i.esClient.RemoveDocument(id); err != nil {
			return err
		}
	}
	return nil
}

func isProductActivity(activity activities.ISiteActivity) bool {
	at := activity.Type()
	return at == productCreatedActivity || at == productUpdatedActivity ||
		at == productArchivedActivity

}

func (i Indexer) Run(activity activities.ISiteActivity) error {
	if !isProductActivity(activity) {
		return nil
	}

	log.Printf("Processing: %s", activity.Data())

	bt := []byte(activity.Data())
	prod := new(ConsumerProduct)

	if err := json.Unmarshal(bt, prod); err != nil {
		return fmt.Errorf("Error unmarshalling activity data into product with error: %s", err.Error())
	}

	if activity.Type() == productArchivedActivity {
		return i.onArchiveProduct(prod)
	}

	existingRows, err := i.existingSearchRows(prod.Product.ID)
	if err != nil {
		return err
	}

	if !prod.Product.IsActive() {
		for id := range existingRows {
			log.Printf("Delete inactive product %d, %s", prod.Product.ID, id)
			if err := i.esClient.RemoveDocument(id); err != nil {
				return err
			}
		}
		return nil
	}

	searchConfig, err := i.FetchSearchConfig()
	if err != nil {
		return err
	}
	searchConfig.VisualVariants = i.VisualVariants

	log.Printf("searchConfig: %v\n", searchConfig)

	partialProducts, err := search.MakePartialProducts(prod.Product, i.VisualVariants)
	if err != nil {
		return fmt.Errorf("Error creating partial products with error: %s", err.Error())
	}

	for _, p := range partialProducts {
		row, err := search.NewSearchRow(prod.Product, p)
		productMap, err := search.EnrichRowWithAttributes(prod.Product, row, searchConfig, p)
		if err != nil {
			log.Printf("Unable to create search row with error: %s", err.Error())
			return err
		}

		if err := i.esClient.UpdateDocument(row.Identifier(), productMap); err != nil {
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
