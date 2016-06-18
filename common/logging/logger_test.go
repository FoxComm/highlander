package logging

import (
	"errors"
	"io/ioutil"
	"os"
	"regexp"
	"testing"

	"github.com/Sirupsen/logrus"

	"github.com/FoxComm/middlewarehouse/common"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
)

func TestLogging(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Logging Tests")
}

var (
	originalEnv string

	// originalStdout is the existing os.Stdout
	originalStdout *os.File

	// stdout is the tempfile we create to redirect Stdout
	stdout *os.File
)

var _ = BeforeSuite(func() {
	originalEnv = os.Getenv("GOENV")
	os.Setenv("GOENV", "test2")
})

var _ = AfterSuite(func() {
	os.Setenv("GOENV", originalEnv)

	// close the Pipe and reset os.Stdout
	stdout.Close()
	os.Stdout = originalStdout
})

var _ = Describe("The Logger", func() {
	AfterEach(func() {
		destroyTestLogfile(common.AppDir() + "/logs/testservice.test2.log")
	})

	It("creates a log file", func() {
		_ = NewLogger("TestService", "debug")
		_, err := os.Open(common.AppDir() + "/logs/testservice.test2.log")
		Expect(err).NotTo(HaveOccurred())
	})

	It("Makes a basic log statement", func() {
		l := NewLogger("TestService", "info")
		l.Infof("testing ...")
		result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing")
		Expect(err).NotTo(HaveOccurred())
		Expect(result).To(BeTrue(), "Log file contains log message.")
	})

	It("Ignores lower logging levels", func() {
		l := NewLogger("TestService", "warn")
		l.Debugf("testing debug")
		result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing debug")
		Expect(err).NotTo(HaveOccurred())
		Expect(result).To(BeFalse(), "Log file set to warn recorded a debug log level")
	})

	It("logs the same log level", func() {
		l := NewLogger("TestService", "warn")
		l.Warnf("testing warn")
		result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing warn")
		Expect(result).To(BeTrue(), "Log file set to warn didnt record a warning log msg.")
	})

	It("logs a higher log level", func() {
		l := NewLogger("TestService", "warn")
		l.Errorf("testing error")
		result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing error")
		Expect(result).To(BeTrue(), "Log file set to warn didnt record an error log msg.")
	})

	It("logs extra fields", func() {
		l := NewLogger("TestService", "warn")
		l.Errorf("testing error", M{"hello": "there"})
		result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "hello")
		Expect(result).To(BeTrue(), "Log file set to error didnt record extra fields in map.")
		result2, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "there")
		Expect(result2).To(BeTrue(), "Log file set to error didnt record extra fields in map.")
	})

	It("Supports convenience function E to make logging errors easier", func() {
		l := NewLogger("TestService", "warn")
		err := errors.New("Hello, I'll be your error today")
		l.Errorf("Testing errors now", E(err))
		result, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "errorMsg")
		Expect(result).To(BeTrue(), "Log file set to warn didnt record an error log msg.")
		result2, _ := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "Hello, I'll be your error today")
		Expect(result2).To(BeTrue(), "Log file set to warn didnt record an error log msg.")
	})

	It("transparently logs to an array of loggers", func() {
		l := NewLogger("TestService", "warn")
		log2, _ := newLogrus("debug")
		f, _ := logfile("second_logger", "test2")
		log2.Out = f

		wrapper := l.(*logrusWrapper)
		wrapper.loggers = []*logrus.Logger{wrapper.loggers[0], log2}

		l.Errorf("testing error")
		result, _ := parsePatternFromLog(common.AppDir()+"/logs/second_logger.test2.log", "testing")
		Expect(result).To(BeTrue(), "Second logger (representing the os.Stdout logger) is not working.")
		destroyTestLogfile(common.AppDir() + "/logs/second_logger.test2.log")
	})

	It("creates a log file with Panicf", func() {
		l := NewLogger("TestService", "panic")

		defer func() {
			err := recover()
			Expect(err).NotTo(BeNil())
			_, ok := err.(*logrus.Entry)
			Expect(ok).To(BeTrue())
		}()

		l.Panicf("panicking! ...")
		result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "panicking")
		Expect(err).NotTo(HaveOccurred())
		Expect(result).To(BeTrue(), "Log file contains log message.")
	})

})

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
