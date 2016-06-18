/*
	A wrapper around `github.com/Sirupsen/logrus`.

	Logrus wants a specialised DSL for its logging:

	l.logrus.WithFields(M{ "req": req }).Debug(msg)

	If we commit to this and want to back out later, we will have a substantial recoding effort, hence the wrapper.

	Importantly, the `Logger` interface is built for two types of calls:

	1) logger.Debugf("Just a simple string")

	2) logger.Debugf("A message with a map of values", logger.M{ "req": req })

	Note - it is possible to continually add maps at the end of the log statement but, if keys collide, the later key will win.

	3) logger.Debugf("A message with, stupidly, two map of values with the same key", logger.M{ "req": req }, logger.M{ "req" : anotherReq })

	This log statement would result in a single "req" mapping to `anotherReq`.
*/

package logging

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/FoxComm/middlewarehouse/common"
	"github.com/Sirupsen/logrus"
)

type Logger interface {
	Debugf(msg string, args ...M)
	Infof(msg string, args ...M)
	Warnf(msg string, args ...M)
	Errorf(msg string, args ...M)
	Fatalf(msg string, args ...M)
	Panicf(msg string, args ...M)
}

var Log Logger

type M map[string]interface{}

//a convenient abbreviation
var E = ErrorToMap

// ErrorToMap is a shorthand convenience function that takes error objects and converts them to a map for a log statement.
// e.g. logger.Errorf("Error occurred signing in!", logger.E(err))
// This is equivalent to - logger.Errorf("Error occurred signing in!", logger.M{ "errorMsg": err.Error(), "error": err })
func ErrorToMap(err error) M {
	return M{
		"errorMsg": err.Error(),
		"error":    err,
	}
}

type logrusWrapper struct {
	loggers []*logrus.Logger
}

func (l *logrusWrapper) Debugf(msg string, args ...M) {
	if len(args) >= 1 {
		for _, logger := range l.loggers {
			logger.WithFields(coalesceMaps(args...)).Debug(msg)
		}
	} else {
		for _, logger := range l.loggers {
			logger.Debugf(msg)
		}
	}
}

func (l *logrusWrapper) Infof(msg string, args ...M) {
	if len(args) == 1 {
		for _, logger := range l.loggers {
			logger.WithFields(coalesceMaps(args...)).Info(msg)
		}
	} else {
		for _, logger := range l.loggers {
			logger.Infof(msg)
		}
	}
}

func (l *logrusWrapper) Warnf(msg string, args ...M) {
	if len(args) == 1 {
		for _, logger := range l.loggers {
			logger.WithFields(coalesceMaps(args...)).Warning(msg)
		}
	} else {
		for _, logger := range l.loggers {
			logger.Warningf(msg)
		}
	}
}

func (l *logrusWrapper) Errorf(msg string, args ...M) {
	if len(args) == 1 {
		for _, logger := range l.loggers {
			logger.WithFields(coalesceMaps(args...)).Error(msg)
		}
	} else {
		for _, logger := range l.loggers {
			logger.Errorf(msg)
		}
	}
}

func (l *logrusWrapper) Fatalf(msg string, args ...M) {
	if len(args) == 1 {
		for _, logger := range l.loggers {
			logger.WithFields(coalesceMaps(args...)).Fatal(msg)
		}
	} else {
		for _, logger := range l.loggers {
			logger.Fatalf(msg)
		}
	}
}

func (l *logrusWrapper) Panicf(msg string, args ...M) {
	if len(args) == 1 {
		for _, logger := range l.loggers {
			logger.WithFields(coalesceMaps(args...)).Panic(msg)
		}
	} else {
		for _, logger := range l.loggers {
			logger.Panicf(msg)
		}
	}
}

//Loggers are singletons at the level of a service. Once it is created with a service name
//further requests for the same service return the same Logger.
var namedLoggers map[string]Logger

func NewLogger(name, level string) Logger {
	var l *logrusWrapper

	if level == "" {
		level = "debug"
		log.Println("LOG_LEVEL has not been set as an env var. Defaulting to debug. This is not suitable for production.")
	}

	log, err := newLogrus(level)

	//default formatter
	log.Formatter = &logrus.TextFormatter{FullTimestamp: true}

	if err != nil {
		panic("Unknown logging level [" + level + "].")
	}

	if common.IsProduction() {
		log.Formatter = new(logrus.JSONFormatter)
		l = &logrusWrapper{loggers: []*logrus.Logger{log}}
		f, _ := logfile(name, common.Env())
		log.Out = f
	} else if common.IsTest() || common.IsEnv("ci") {
		//for test we just use stdout
		l = &logrusWrapper{loggers: []*logrus.Logger{log}}
	} else {
		//and we keep using the default text formatter
		stdOutLog, _ := newLogrus(level) //ignore this error condition, we've already enacted it when creating the first logger
		stdOutLog.Formatter = &logrus.TextFormatter{FullTimestamp: true}
		l = &logrusWrapper{loggers: []*logrus.Logger{log, stdOutLog}}
		f, _ := logfile(name, common.Env())
		log.Out = f
	}

	return Logger(l)
}

func newLogrus(level string) (l *logrus.Logger, err error) {
	l = logrus.New()
	logLevel, err := logrus.ParseLevel(level)

	if err != nil {
		return
	}
	l.Level = logLevel

	return
}

func logfile(name, env string) (f *os.File, err error) {
	name = strings.ToLower(name)
	middlewarehouseBasePath := common.AppDir()
	filename := filepath.Join(middlewarehouseBasePath, "logs", fmt.Sprintf("%s.%s.log", name, env))

	f, err = os.OpenFile(filename, os.O_RDWR|os.O_APPEND, os.ModeAppend)

	if err != nil {
		fmt.Println(fmt.Sprintf("Creating %s", filename))

		err = os.Mkdir(fmt.Sprintf("%s/logs", middlewarehouseBasePath), 0770)
		f, err = os.Create(filename)

		if err != nil {
			fmt.Println(fmt.Sprintf("Critical. Could not create log file. Panicking - [%s]", err.Error()))
			panic(err)
		}
	}

	return
}

func coalesceMaps(args ...M) map[string]interface{} {
	var m M

	if len(args) > 1 {
		m = make(M)

		for _, arg := range args {
			m = clobberMerge(m, arg)
		}
	} else {
		return args[0]
	}

	return m
}

func clobberMerge(dest M, src M) M {
	for k, v := range src {
		dest[k] = v
	}
	return dest
}

func init() {
	logLevel := os.Getenv("LOG_LEVEL")
	Log = NewLogger("middlewarehouse", logLevel)
}
