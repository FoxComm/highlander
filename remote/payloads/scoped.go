package payloads

type Scoped interface {
	EnsureScope(string)
}
