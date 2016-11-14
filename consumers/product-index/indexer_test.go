package main

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/FoxComm/highlander/consumers/product-index/fixtures"
	"github.com/FoxComm/highlander/consumers/product-index/utils"
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
	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductCreated)
	suite.Nil(err)

	es := utils.ElasticServer{}
	ts := httptest.NewServer(http.HandlerFunc(es.ServeHTTP))
	defer ts.Close()

	idx, err := NewIndexer(ts.URL, "public", "products_catalog_view", []string{"color"})
	suite.Nil(err)

	err = idx.Run(fullProduct)
	suite.Nil(err)

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
