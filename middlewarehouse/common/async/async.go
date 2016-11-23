package async

import (
	"log"

	"github.com/FoxComm/highlander/middlewarehouse/common/exceptions"
)

// AsyncFn is a function that may be executed asynchronously (through a GoRoutine).
// It returns an error, with is always nil if run async, but used if run synchronously.
type AsyncFn func() exceptions.IException

// MaybeExecAsync will conditionally run a function synchronously or asynchronously.
func MaybeExecAsync(fn AsyncFn, isAsync bool, logMsg string) exceptions.IException {
	if isAsync {
		go func() {
			if exception := fn(); exception != nil {
				log.Printf("%s: %s", logMsg, exception.ToString())
			}
		}()
		return nil
	}

	return fn()
}
