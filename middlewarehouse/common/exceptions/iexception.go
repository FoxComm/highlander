package exceptions

type IException interface {
	ToString() string
	ToJSON() interface{}
}

type Exception struct {
	Message string `json:"message"`
}

func (exception Exception) ToString() string {
	return exception.Message
}
