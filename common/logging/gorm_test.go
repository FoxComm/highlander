package logging

import (
	"testing"

	"github.com/FoxComm/middlewarehouse/common"
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"
)

func TestGormLogging(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Gorm Logging Tests")
}

var _ = Describe("The Logger", func() {
	AfterEach(func() {
		destroyTestLogfile(common.AppDir() + "/logs/testservice.test2.log")
	})

	It("Makes a basic log statement", func() {
		l := NewLogger("TestService", "debug")
		gl := NewGormLogger(l)
		gl.Print("testing ...")
		result, err := parsePatternFromLog(common.AppDir()+"/logs/testservice.test2.log", "testing")
		Expect(err).NotTo(HaveOccurred())
		Expect(result).To(BeTrue(), "Log file contains log message.")
	})
})
