package main

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/highlander/consumers/product-index/fixtures"
	"github.com/FoxComm/highlander/consumers/product-index/mocks"
	"github.com/FoxComm/highlander/consumers/product-index/search"
	"github.com/FoxComm/highlander/shared/golang/activities"
	"github.com/stretchr/testify/suite"
)

type IndexerTestSuite struct {
	suite.Suite
}

func TestIndexerSuite(t *testing.T) {
	suite.Run(t, new(IndexerTestSuite))
}

func (suite *IndexerTestSuite) TestCreateProductNoVariants() {
	product := fixtures.NewProductWithNoVariants()
	prodIdx := fmt.Sprintf("%d-%d", product.ID, product.SKUs[0].ID)

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductCreated)
	suite.Nil(err)

	index := "public"
	mapping := "products_catalog_view"
	visualVariants := []string{"color"}
	rows := map[string]search.SearchRow{}

	es := mocks.NewElasticServer(index, mapping, rows)
	ts := httptest.NewServer(http.HandlerFunc(es.ServeHTTP))
	defer ts.Close()

	indexer, err := NewIndexer(ts.URL, index, mapping, visualVariants)
	suite.Nil(err)

	err = indexer.Run(fullProduct)
	suite.Nil(err)

	suite.Len(es.Rows, 1)

	_, ok := es.Rows[prodIdx]
	suite.True(ok)
}

func (suite *IndexerTestSuite) TestUpdateProductNoVariants() {
	return
}

func (suite *IndexerTestSuite) TestCreateProductOneVisualVariant() {
	return
}

func (suite *IndexerTestSuite) TestUpdateProductOneVisualVariant() {
	return
}

func (suite *IndexerTestSuite) TestCreateProductMultipleVisualOneNonVisualVariant() {
	return
}

func (suite *IndexerTestSuite) TestUpdateProductMultipleVisualOneNonVisualVariant() {
	return
}

func (suite *IndexerTestSuite) TestCreateProductMultipleVisualVariants() {
	return
}

func (suite *IndexerTestSuite) TestUpdateProductMultipleVisualVariants() {
	return
}
