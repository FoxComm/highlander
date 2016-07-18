package validation

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"testing"
)

type InvalidTestSuite struct {
	suite.Suite
	invalids []Invalid
}

func TestInvalidSuite(t *testing.T) {
	suite.Run(t, new(InvalidTestSuite))
}

func (suite *InvalidTestSuite) SetupTest() {
	suite.invalids = []Invalid{
		Invalid{Field: "Foo", Err: "one two three"},
		Invalid{Field: "Foo2", Err: "four five six"},
	}
}

func (suite *InvalidTestSuite) TestAreCollated() {
	in := Invalids(suite.invalids)
	msg := in.String()
	assert.Equal(suite.T(), "Foo: one two three\nFoo2: four five six\n", msg)
}
