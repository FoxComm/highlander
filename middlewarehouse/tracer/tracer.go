package tracer

import (
	"fmt"

	"github.com/opentracing/opentracing-go"
	zipkin "github.com/openzipkin/zipkin-go-opentracing"
)

func NewTracer() (opentracing.Tracer, error) {

	collector, err := zipkin.NewHTTPCollector("http://10.240.0.5:9411/api/v1/spans")
	if err != nil {
		return nil, fmt.Errorf("Failed to start kafka collector with error %s", err.Error())
	}

	tracer, err := zipkin.NewTracer(
		zipkin.NewRecorder(collector, true, ":9292", "middlewarehouse"),
		zipkin.DebugMode(true),
	)
	if err != nil {
		return nil, fmt.Errorf("Failed to start zipkin tracer with error %s", err.Error())
	}

	return tracer, nil
}
