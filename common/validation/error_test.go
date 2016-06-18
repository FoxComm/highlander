package validation

import (
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"

	"testing"
)

func TestInvalidError(t *testing.T) {
	RegisterFailHander(Fail)
	RunSpecs(t, "Invalid Error Test")
}

var _ = Context("Invalid Error Type", func() {
	var (
		invalids []Invalid
	)

	BeforeEach(func() {
		invalids = []Invalid{
			Invalid{Field: "Foo", Err: "one two three"},
		}
	})

	It("are collated", func() {
		in := Invalids(invalids)
		err := in.Error()
		Expect(err).To(HaveOccurred())
	})
})
