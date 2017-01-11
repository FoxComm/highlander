package tracer

import (
	"fmt"

	"github.com/FoxComm/highlander/middlewarehouse/common/config"
	"github.com/opentracing/opentracing-go"
	zipkin "github.com/openzipkin/zipkin-go-opentracing"
	"time"
)

func NewTracer(config *config.TracerConfig, port string) (opentracing.Tracer, error) {
	collector, err := zipkin.NewScribeCollector(config.ZipkinServerURL, 2*time.Second)
	if err != nil {
		return nil, fmt.Errorf("Failed to start zipkin collector with error %s", err.Error())
	}

	recorder := zipkin.NewRecorder(collector, false, fmt.Sprintf("localhost:%s", port), "middlewarehouse")
	tracer, err := zipkin.NewTracer(recorder)
	if err != nil {
		return nil, fmt.Errorf("Failed to start zipkin tracer with error %s", err.Error())
	}

	return tracer, nil
}
