/*
	Current conventions.
	Logger uses `os.Getenv("GOENV")` to determine an application environment.
	Production logging is in JSON, no stdout.
	Test logging is in TextFormat, only stdout.
	Other logging is in TextFormat, stdout and filed based.
	Loggers have names that should map to our evolving notion of modules/applications/services (etc).
	To configure logging just ensure you import it. A global Log object will be available via `logging.Log`.
*/

package logging
