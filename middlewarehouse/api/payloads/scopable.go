package payloads

type IScopable interface {
    GetScope() string
    SetScope(string)
}

type Scopable struct {
    Scope string `json:"scope"`
}

func (scopable *Scopable) GetScope() string {
    return scopable.Scope
}

func (scopable *Scopable) SetScope(scope string) {
    scopable.Scope = scope
}
