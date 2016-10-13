# metamorphosis #

metamorphosis is a Go client for easily interacting with Kafka. It works best
when used to handle a Kafka setup that's clustered with Zookeeper and whose
messages are encoded with Avro.

## Usage ##

```go
import "github.com/FoxComm/metamorphosis
```

Construct a new consumer by creating a Consumer that connects to Zookeeper and
the Avro schema registry.

For example:

```go
zookeeper := "localhost:2181"
schemaRepo := "http://localhost:8081"

consumer, err := metamorphosis.NewConsumer(zookeeper, schemaRepo)
```

To handle messages, define a handler and run against a topic:

```go
handler := func(message AvroMessage) error {
  bytes := message.Bytes()
  fmt.Println(string(bytes))
  return nil
}

consumer.RunTopic("my_topic", 1, handler)
```

## License ##

MIT
