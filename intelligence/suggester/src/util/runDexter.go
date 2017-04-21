package util

import (
	"os"
	/*
		"encoding/json"
		"fmt"
		"net/http"
		"net/url"
		"strconv"
		"strings"
	*/

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
)

var (
	runDexterBotID  string = os.Getenv("DEXTER_BOT_ID")
	runDexterApiKey string = os.Getenv("DEXTER_API_KEY")
)

func DexterSuggestionToSMS(phoneNumber string, imageUrl string, product responses.ProductInstance) (responses.RunDexterResponse, error) {
	return responses.RunDexterResponse{}, nil
}
