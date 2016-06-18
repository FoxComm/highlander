package validation

import (
	. "github.com/onsi/ginkgo"
	. "github.com/onsi/gomega"

	"testing"
)

func TestInvalid(t *testing.T) {
	RegisterFailHandler(Fail)
	RunSpecs(t, "Invalid Test")
}

var _ = Context("Error messages", func() {

	var (
		invalids []Invalid
	)

	BeforeEach(func() {
		invalids = []Invalid{
			Invalid{Field: "Foo", Err: "one two three"},
			Invalid{Field: "Foo2", Err: "four five six"},
		}
	})

	It("are collated", func() {
		in := Invalids(invalids)
		msg := in.String()
		Expect(msg).To(Equal("Foo: one two three\nFoo2: four five six\n"))
	})
})
