package main

import (
	"testing"

	"github.com/stretchr/testify/suite"
)

type IndexerTestSuite struct {
	suite.Suite
}

func TestIndexerSuite(t *testing.T) {
	suite.Run(t, new(IndexerTestSuite))
}

func (suite *IndexerTestSuite) TestCreateProductNoVariants() {
	return
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
