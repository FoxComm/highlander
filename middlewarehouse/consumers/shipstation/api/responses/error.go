package responses

// Error represents an error response from ShipStation.
type Error struct {
	Message          string
	ExceptionMessage string
	ExceptionType    string
	StackTrace       string
	InnerException   innerException
}

type innerException struct {
	Message          string
	ExceptionMessage string
	ExceptionType    string
	StackTrace       string
}
