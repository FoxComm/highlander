package tracer

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/config"
	"github.com/opentracing/opentracing-go"
	zipkin "github.com/openzipkin/zipkin-go-opentracing"
)

func NewTracer(config *config.TracerConfig, port string) (opentracing.Tracer, error) {

	collector, err := zipkin.NewHTTPCollector(fmt.Sprintf("%s/api/v1/spans", config.ZipkinHttpEndpoint))
	if err != nil {
		return nil, fmt.Errorf("Failed to start zipkin collector with error %s", err.Error())
	}

	tracer, err := zipkin.NewTracer(
		zipkin.NewRecorder(collector, true, fmt.Sprintf("localhost:%s", port), "middlewarehouse"),
		zipkin.DebugMode(true),
	)
	if err != nil {
		return nil, fmt.Errorf("Failed to start zipkin tracer with error %s", err.Error())
	}

	return tracer, nil
}
