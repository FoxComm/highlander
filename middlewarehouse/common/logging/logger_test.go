package logging

import (
	"errors"
	"io/ioutil"
	"os"
	"regexp"
	"testing"

	"github.com/Sirupsen/logrus"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"

	"github.com/FoxComm/middlewarehouse/common"
)

type LoggingTestSuite struct {
	suite.Suite
}

func TestLogging(t *testing.T) {
	suite.Run(t, new(LoggingTestSuite))
}

var (
	originalEnv string

	// originalStdout is the existing os.Stdout
	originalStdout *os.File

	// stdout is the tempfile we create to redirect Stdout
	stdout *os.File
)

func (suite *LoggingTestSuite) SetupSuite() {
	originalEnv = os.Getenv("GOENV")
	os.Setenv("GOENV", "test2")
}

func (suite *LoggingTestSuite) TearDownSuite() {
	os.Setenv("GOENV", originalEnv)

	// close the Pipe and reset os.Stdout
	stdout.Close()
	os.Stdout = originalStdout
}

func (suite *LoggingTestSuite) TearDownTest() {
	destroyTestLogfile(common.AppDir() + "/logs/testservice.test2.log")
}

func (suite *LoggingTestSuite) TestCreateLogFile() {
	_ = NewLogger("TestService", "debug")
	_, err := os.Open(common.AppDir() + "/logs/testservice.test2.log")
	assert.Nil(suite.T(), err)
}

func (suite *LoggingTestSuite) TestBasicLogStatement() {
	l := NewLogger("TestService", "info")
	l.Infof("testing ...")
	result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing")
	if assert.Nil(suite.T(), err) {
		assert.True(suite.T(), result, "Log file contains log message.")
	}
}

func (suite *LoggingTestSuite) TestLowerLoggingLevel() {
	l := NewLogger("TestService", "warn")
	l.Debugf("testing debug")
	result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing debug")
	if assert.Nil(suite.T(), err) {
		assert.False(suite.T(), result, "Log file set to warn recorded a debug log level")
	}
}

func (suite *LoggingTestSuite) TestSameLoggingLevel() {
	l := NewLogger("TestService", "warn")
	l.Warnf("testing warn")
	result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing warn")
	assert.True(suite.T(), result, "Log file set to warn didnt record a warning log msg.")
}

func (suite *LoggingTestSuite) TestHigherLoggingLevel() {
	l := NewLogger("TestService", "warn")
	l.Errorf("testing error")
	result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing error")
	assert.True(suite.T(), result, "Log file set to warn didnt record an error log msg.")
}

func (suite *LoggingTestSuite) TestExtraFields() {
	l := NewLogger("TestService", "warn")
	l.Errorf("testing error", M{"hello": "there"})
	result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "hello")
	assert.True(suite.T(), result, "Log file set to error didnt record extra fields in map.")
	result2, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "there")
	assert.True(suite.T(), result2, "Log file set to error didnt record extra fields in map.")
}

func (suite *LoggingTestSuite) TestConvenienceFnE() {
	l := NewLogger("TestService", "warn")
	err := errors.New("Hello, I'll be your error today")
	l.Errorf("Testing errors now", E(err))
	result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "errorMsg")
	assert.True(suite.T(), result, "Log file set to warn didnt record an error log msg.")
	result2, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "Hello, I'll be your error today")
	assert.True(suite.T(), result2, "Log file set to warn didnt record an error log msg.")
}

func (suite *LoggingTestSuite) TestArrayOfLoggers() {
	l := NewLogger("TestService", "warn")
	log2, _ := newLogrus("debug")
	f, _ := logfile("second_logger", "test2")
	log2.Out = f

	wrapper := l.(*logrusWrapper)
	wrapper.loggers = []*logrus.Logger{wrapper.loggers[0], log2}

	l.Errorf("testing error")
	result, _ := parsePatternFromLog(common.AppDir()+"/logs/second_logger.test2.log", "testing")
	assert.True(suite.T(), result, "Second logger (representing the os.Stdout logger) is not working.")
	destroyTestLogfile(common.AppDir() + "/logs/second_logger.test2.log")
}

func (suite *LoggingTestSuite) TestLogPanicf() {
	l := NewLogger("TestService", "panic")

	defer func() {
		err := recover()
		assert.NotNil(suite.T(), err)
		_, ok := err.(*logrus.Entry)
		assert.True(suite.T(), ok)
	}()

	l.Panicf("panicking! ...")
	result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "panicking")
	assert.Nil(suite.T(), err)
	assert.True(suite.T(), result, "Log file contains log message.")
}

func destroyTestLogfile(filename string) {
	err := os.Remove(filename)
	if err != nil {
		panic("Couldnt cleaup test logfile: " + err.Error())
	}
}

func parsePatternFromLog(file, pattern string) (match bool, err error) {
	data, err := ioutil.ReadFile(file)
	match, _ = regexp.Match(pattern, data)
	return
}
