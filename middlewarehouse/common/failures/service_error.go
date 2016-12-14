package failures

import (
	"fmt"
	"log"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/jinzhu/gorm"
)

func HandleServiceError(context *gin.Context, err error) {
	fail := getFailure(err)

	logFailure(fail)

	Abort(context, fail)
}

func getFailure(err error) Failure {
	if err == gorm.ErrRecordNotFound {
		return NewNotFound(err)
	}

	return NewBadRequest(err)
}

func logFailure(fail Failure) {
	messages := []string{}
	for _, err := range fail.ToJSON().Errors {
		messages = append(messages, fmt.Sprintf("ServiceError: %s", err))
	}

	log.Println(strings.Join(messages, "\n"))
}
