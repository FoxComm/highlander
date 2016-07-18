package logging

import "fmt"

// GormLogger is a logger that's explicitly used for printing output from Gorm.
// It's specifically a wrapper for our current logging infrastructure that
// conforms to the standard logging interface that Gorm accepts.
type GormLogger interface {
	Print(values ...interface{})
}

// NewGormLogger instantiates a new instance of GormLogger.
func NewGormLogger(l Logger) GormLogger {
	return &gormLogger{log: l}
}

type gormLogger struct {
	log Logger
}

func (g *gormLogger) Print(values ...interface{}) {
	message := fmt.Sprint(values)
	g.log.Debugf(message)
}
