package metamorphosis

// Handler defines the function signature for handling the contents of a
// decoded Avro message.
type Handler func(message AvroMessage) error
