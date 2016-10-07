package tests

import (
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
