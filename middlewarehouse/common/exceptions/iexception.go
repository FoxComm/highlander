package exceptions

type IException interface {
	ToString() string
	ToJSON() interface{}
}

type Exception struct {
	Error error
}

func (exception Exception) ToString() string {
	return exception.Error.Error()
}

func (exception Exception) ToJSON() interface{} {
	return exception
}