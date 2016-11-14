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

const index = "public"
const mapping = "products_catalog_view"

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
	product := fixtures.NewProductWithNoVariants()
	prodIdx := fmt.Sprintf("%d-%d", product.ID, product.SKUs[0].ID)

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductUpdated)
	suite.Nil(err)

	visualVariants := []string{"color"}
	row := makeSearchRowNoVariants(1, 2, "TEST-SKU")
	rows := map[string]search.SearchRow{"1-2": row}

	es := mocks.NewElasticServer(index, mapping, rows)
	ts := httptest.NewServer(http.HandlerFunc(es.ServeHTTP))
	defer ts.Close()

	indexer, err := NewIndexer(ts.URL, index, mapping, visualVariants)
	suite.Nil(err)

	err = indexer.Run(fullProduct)
	suite.Nil(err)

	suite.Len(es.Rows, 1)

	updatedRow, ok := es.Rows[prodIdx]
	suite.True(ok)

	expectedSalePrice, err := product.SKUs[0].SalePrice()
	suite.Nil(err)

	suite.Equal(expectedSalePrice.Value, updatedRow.SalePrice)
}

func (suite *IndexerTestSuite) TestUpdateProduceNoVariantsReplaceSKU() {
	product := fixtures.NewProductWithNoVariants()
	prodIdx := fmt.Sprintf("%d-%d", product.ID, product.SKUs[0].ID)

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductUpdated)
	suite.Nil(err)

	visualVariants := []string{"color"}
	row := makeSearchRowNoVariants(1, 3, "ANOTHER-SKU")
	rows := map[string]search.SearchRow{"1-3": row}

	es := mocks.NewElasticServer(index, mapping, rows)
	ts := httptest.NewServer(http.HandlerFunc(es.ServeHTTP))
	defer ts.Close()

	indexer, err := NewIndexer(ts.URL, index, mapping, visualVariants)
	suite.Nil(err)

	err = indexer.Run(fullProduct)
	suite.Nil(err)

	suite.Len(es.Rows, 1)

	updatedRow, ok := es.Rows[prodIdx]
	suite.True(ok)

	expectedSalePrice, err := product.SKUs[0].SalePrice()
	suite.Nil(err)

	suite.Equal(expectedSalePrice.Value, updatedRow.SalePrice)
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

//
// Some fixtures to make testing easier.
//
func makeSearchRowNoVariants(productID int, skuID int, code string) search.SearchRow {
	return search.SearchRow{
		ProductID: productID,
		Context:   "default",
		SKUs: []search.SearchSKU{
			search.SearchSKU{ID: skuID, Code: code},
		},
		Title:       "A test SKU",
		Description: "<p>Some test SKU</p>",
		Image:       "http://lorempixel.com/400/200",
		SalePrice:   999,
		Currency:    "USD",
		Tags:        []string{"Sample"},
	}
}
