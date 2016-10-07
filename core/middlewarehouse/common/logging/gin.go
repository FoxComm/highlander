package logging

import (
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

func GinLogger(l Logger) gin.HandlerFunc {
	return func(c *gin.Context) {
		t := time.Now()
		c.Next()

		// after request
		latency := time.Since(t)
		status := c.Writer.Status()
		l.Infof(strconv.Itoa(status), M{"latency": latency})
	}
}
