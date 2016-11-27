package tests

import (
	"encoding/json"
	"fmt"
	"reflect"
	"time"
)

func SyncDates(models ...interface{}) {
	for _, field := range []string{"CreatedAt", "UpdatedAt"} {
		value := getTime(models[0], field)
		for i := range models {
			setTime(models[i], field, value)
		}
	}
}

func getField(model interface{}, field string) reflect.Value {
	return reflect.Indirect(reflect.ValueOf(model)).FieldByName("Base").FieldByName(field)
}

func setTime(model interface{}, field string, value reflect.Value) {
	getField(model, field).Set(value)
}

func getTime(model interface{}, field string) reflect.Value {
	return getField(model, field).MethodByName("Round").Call([]reflect.Value{reflect.ValueOf(time.Microsecond)})[0]
}

func ToString(model interface{}) string {
	//deserialization is used to workaround keys order issue
	//struct, being serialized, has keys in order, given in declaration
	//map - in alphabetical order
	//struct, being, serialized, deserialized and serialized again behaves same as map with same keys and values
	return toString(fromString(toString(model)))
}

func fromString(string string) interface{} {
	model := new(interface{})
	json.Unmarshal([]byte(string), model)

	fmt.Println(model)

	return model
}

func toString(model interface{}) string {
	data, _ := json.Marshal(model)

	return string(data)
}
