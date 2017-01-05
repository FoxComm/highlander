package errors

// DatabaseError wraps an error returned by the PostgreSQL driver and provides
// tools to return a sanitized error to the caller and record debugging
// information for logging.
type DatabaseError struct{}
