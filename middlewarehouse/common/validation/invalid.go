package validation

import "fmt"

const (
	ValErrors = "Validation failures identified"
)

// Invalid describes a validation error belonging to a specific field.
// see https://github.com/ArdanStudios/gotraining/blob/master/12-http/api/app/context.go
type Invalid struct {
	Field string `json:"field_name"`
	Err   string `json:"error"`
}

type Invalids []Invalid

func (in Invalids) String() string {
	errStr := ""

	for _, invalid := range in {
		errStr += fmt.Sprintf("%s: %s\n", invalid.Field, invalid.Err)
	}

	return errStr
}

func (in Invalids) Error() error {
	return invalidErr{Invalids: in}
}

func Err(in []Invalid) error {
	return Invalids(in).Error()
}
