package validation

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"testing"
)

type ErrorTestSuite struct {
	suite.Suite
	invalids []Invalid
}

func TestErrorSuite(t *testing.T) {
	suite.Run(t, new(ErrorTestSuite))
}

func (suite *ErrorTestSuite) SetupTest() {
	suite.invalids = []Invalid{
		Invalid{Field: "Foo", Err: "one two three"},
	}
}

func (suite *ErrorTestSuite) TestAreCollated() {
	in := Invalids(suite.invalids)
	err := in.Error()
	assert.NotNil(suite.T(), err)
}
