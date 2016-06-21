package logging

import (
	"os"
	"testing"

	"github.com/FoxComm/middlewarehouse/common"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type GormTestSuite struct {
	suite.Suite
}

func TestGormLogging(t *testing.T) {
	suite.Run(t, new(GormTestSuite))
}

func (suite *GormTestSuite) SetupSuite() {
	originalEnv = os.Getenv("GOENV")
	os.Setenv("GOENV", "test2")
}

func (suite *GormTestSuite) TearDownSuite() {
	os.Setenv("GOENV", originalEnv)

	// close the Pipe and reset os.Stdout
	stdout.Close()
	os.Stdout = originalStdout
}

func (suite *GormTestSuite) TestTearDown() {
	destroyTestLogfile(common.AppDir() + "/logs/testservice.test2.log")
}

func (suite *GormTestSuite) TestBasicLogStatement() {
	l := NewLogger("TestService", "debug")
	gl := NewGormLogger(l)
	gl.Print("testing ...")
	result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing")
	assert.Nil(suite.T(), err)
	assert.True(suite.T(), result, "Log file contains log message.")
}
