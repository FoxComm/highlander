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
	prodIndicies := []string{fmt.Sprintf("%d-%d", product.ID, product.SKUs[0].ID)}

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductCreated)
	suite.Nil(err)

	visualVariants := []string{"color"}
	rows := map[string]search.SearchRow{}

	suite.testProductIndexer(fullProduct, rows, prodIndicies, visualVariants)
}

func (suite *IndexerTestSuite) TestUpdateProductNoVariants() {
	product := fixtures.NewProductWithNoVariants()
	prodIndicies := []string{fmt.Sprintf("%d-%d", product.ID, product.SKUs[0].ID)}

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductUpdated)
	suite.Nil(err)

	visualVariants := []string{"color"}
	row := makeSearchRowNoVariants(1, "Test Product", 2, "TEST-SKU")
	rows := map[string]search.SearchRow{"1-2": row}

	suite.testProductIndexer(fullProduct, rows, prodIndicies, visualVariants)
}

func (suite *IndexerTestSuite) TestUpdateProduceNoVariantsReplaceSKU() {
	product := fixtures.NewProductWithNoVariants()
	prodIndicies := []string{fmt.Sprintf("%d-%d", product.ID, product.SKUs[0].ID)}

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductUpdated)
	suite.Nil(err)

	visualVariants := []string{"color"}
	row := makeSearchRowNoVariants(1, "Another Product", 3, "ANOTHER-SKU")
	rows := map[string]search.SearchRow{"1-3": row}

	suite.testProductIndexer(fullProduct, rows, prodIndicies, visualVariants)
}

func (suite *IndexerTestSuite) TestCreateProductOneVisualVariant() {
	product := fixtures.NewProductWithOneVisualVariant()
	prodIndicies := []string{"1-2", "1-4"}

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductCreated)
	suite.Nil(err)

	visualVariants := []string{"color"}
	rows := map[string]search.SearchRow{}

	suite.testProductIndexer(fullProduct, rows, prodIndicies, visualVariants)
}

func (suite *IndexerTestSuite) TestUpdateProductOneVisualVariant() {
	product := fixtures.NewProductWithOneVisualVariant()
	prodIndicies := []string{"1-2", "1-4"}

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductCreated)
	suite.Nil(err)

	visualVariants := []string{"color"}

	rows := map[string]search.SearchRow{
		"1-2": makeSearchRowNoVariants(1, "Orange Stuff", 2, "TEST-ORANGE"),
		"1-4": makeSearchRowNoVariants(1, "White Stuff", 4, "TEST-WHITE"),
	}

	suite.testProductIndexer(fullProduct, rows, prodIndicies, visualVariants)
}

func (suite *IndexerTestSuite) TestUpdateProductOneVisualVariantNewVariants() {
	product := fixtures.NewProductWithOneVisualVariant()
	prodIndicies := []string{"1-2", "1-4"}

	fullProduct, err := activities.NewFullProduct(nil, product, activities.ProductCreated)
	suite.Nil(err)

	visualVariants := []string{"color"}

	rows := map[string]search.SearchRow{
		"1-9":  makeSearchRowNoVariants(1, "Orange Stuff", 9, "TEST-ORANGE"),
		"1-10": makeSearchRowNoVariants(1, "White Stuff", 10, "TEST-WHITE"),
	}

	suite.testProductIndexer(fullProduct, rows, prodIndicies, visualVariants)
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

func (suite *IndexerTestSuite) testProductIndexer(
	activity activities.FullProduct,
	rows map[string]search.SearchRow,
	prodIndicies []string,
	visualVariants []string) {

	product := activity.Product()
	es := mocks.NewElasticServer(index, mapping, rows)
	ts := httptest.NewServer(http.HandlerFunc(es.ServeHTTP))
	defer ts.Close()

	indexer, err := NewIndexer(ts.URL, index, mapping, visualVariants)
	suite.Nil(err)

	err = indexer.Run(activity)
	suite.Nil(err)

	suite.Len(es.Rows, len(prodIndicies))
	for _, prodIdx := range prodIndicies {
		updatedRow, ok := es.Rows[prodIdx]
		suite.True(ok)

		expectedSalePrice, err := product.SKUs[0].SalePrice()
		suite.Nil(err)
		suite.Equal(expectedSalePrice.Value, updatedRow.SalePrice)

		expectedTitle, err := product.Title()
		suite.Nil(err)
		suite.Equal(expectedTitle, updatedRow.Title)
	}
}

//
// Some fixtures to make testing easier.
//
func makeSearchRowNoVariants(productID int, title string, skuID int, code string) search.SearchRow {
	return search.SearchRow{
		ProductID: productID,
		Context:   "default",
		SKUs: []search.SearchSKU{
			search.SearchSKU{ID: skuID, Code: code},
		},
		Title:       title,
		Description: "<p>Some test SKU</p>",
		Image:       "http://lorempixel.com/400/200",
		SalePrice:   999,
		Currency:    "USD",
		Tags:        []string{"Sample"},
	}
}
