package payloads

import "github.com/FoxComm/highlander/remote/utils/failures"

type Payload interface {
	Validate() failures.Failure
}
