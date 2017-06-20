package failures

import (
	"fmt"
	"runtime"
)

// Much of the implemenation of retrieving the call stack is inspired by the
// wonderful go-errors package. It simplifies the implementation, as we don't
// need as much flexibility and tailors the stack to our usecase.

// callStack is the set of pointers to the functions that have called the
// error in question.
type callStack []uintptr

const (
	maxStackDepth = 32
	skipFrames    = 3
)

func newCallStack() *callStack {
	var callStackPtrs [maxStackDepth]uintptr

	// Get the pointers, but skip the failure infrastructure.
	actualCount := runtime.Callers(skipFrames, callStackPtrs[:])
	var st callStack = callStackPtrs[0:actualCount]

	return &st
}

func (s *callStack) stackTrace() string {
	var trace string

	for i := 0; i < len(*s); i++ {
		ptr := (*s)[i]

		fnPtr := uintptr(ptr) - 1
		fn := runtime.FuncForPC(fnPtr)

		fnName := fn.Name()
		fileName, fileLine := fn.FileLine(fnPtr)

		trace = fmt.Sprintf(
			"%s%s:%d --> %s\n",
			trace,
			fileName,
			fileLine,
			fnName)
	}

	return trace
}
