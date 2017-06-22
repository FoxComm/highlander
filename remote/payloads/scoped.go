package payloads

type Scoped interface {
	Scope() string
	SetScope(string)
}
