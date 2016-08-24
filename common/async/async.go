package async

import "log"

// AsyncFn is a function that may be executed asynchronously (through a GoRoutine).
// It returns an error, with is always nil if run async, but used if run synchronously.
type AsyncFn func() error

// MaybeExecAsync will conditionally run a function synchronously or synchronously.
func MaybeExecAsync(fn AsyncFn, isAsync bool, logMsg string) error {
	if isAsync {
		go func() {
			if err := fn(); err != nil {
				log.Printf("%s: %s", logMsg, err.Error())
			}
		}()
		return nil
	}

	return fn()
}
