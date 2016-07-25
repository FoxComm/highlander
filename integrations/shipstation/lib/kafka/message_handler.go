package kafka

import "fmt"

// MessageHandler defines the function signature for handling the contents of
// a decoded Avro message.
type MessageHandler func(message *[]byte) error

// PrintMessage simply prints a decoded Avro message to the command line.
func PrintMessage(message *[]byte) error {
	fmt.Println(string(*message))
	return nil
}
