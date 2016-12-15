const { HttpHeaders, Annotation } = require('zipkin');

function getHeaders(traceId, headers) {
  headers[HttpHeaders.TraceId] = traceId.traceId;
  headers[HttpHeaders.SpanId] = traceId.spanId;

  traceId._parentId.ifPresent(psid => {
    headers[HttpHeaders.ParentSpanId] = psid;
  });
  traceId.sampled.ifPresent(sampled => {
    headers[HttpHeaders.Sampled] = sampled ? '1' : '0';
  });

  return headers;
}


export default tracer => serviceName => request => {
  tracer.scoped(() => {
    tracer.setId(tracer.createChildId());

    const traceId = tracer.id;
    const method = request.method || 'GET';

    tracer.recordServiceName(serviceName);
    tracer.recordRpc(`${method.toUpperCase()} ${request.url}`);
    tracer.recordBinary('http.path', request.url);
    tracer.recordAnnotation(new Annotation.ClientSend());

    if (serviceName) {
      // TODO: can we get the host and port of the http connection?
      tracer.recordAnnotation(new Annotation.ServerAddr({
        serviceName: serviceName
      }));
    }

    request.header = getHeaders(traceId, request.header);

    request.on('response', res => {
      tracer.scoped(() => {
        tracer.setId(traceId);
        tracer.recordBinary('http.status_code', res.status.toString());
        if (res.serverError) {
          tracer.recordBinary('http.error', res.error.toString());
        }
        tracer.recordAnnotation(new Annotation.ClientRecv());
      });
    });
  });

  return request;
};
