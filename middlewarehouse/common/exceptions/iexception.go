package exceptions

type IException interface {
	ToString() string
	ToJSON() interface{}
}
