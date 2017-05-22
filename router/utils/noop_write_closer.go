package utils

import "io"

// NoopWriteCloser is a wrapper to any implementation of io.Writer that adds
// (or overrides) the Close() method to be a no-op. Very heavily inspired by
// the nopWriteCloser in from Oxy (github.com/mailgun/oxy).
type NoopWriteCloser struct {
	io.Writer
}

// NewNoopWriteCloser wraps an io.Writer and outputs a new NoopWriteCloser.
func NewNoopWriteCloser(w io.Writer) *NoopWriteCloser {
	return &NoopWriteCloser{w}
}

func (n *NoopWriteCloser) Close() error {
	return nil
}
