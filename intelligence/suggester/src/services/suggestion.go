package services

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
	"github.com/FoxComm/highlander/intelligence/suggester/src/util"
	"github.com/labstack/echo"
)

func selectUpSellAndPushToSms(phoneNumber string, antHillData responses.AntHillResponse) (responses.TwilioSmsResponse, error) {
	if len(antHillData.Products) < 1 {
		return responses.TwilioSmsResponse{}, errors.New("There are no products for potential up-sell")
	}

	// TODO: After testing create a selection algorithm based on score or previous Rejections filter out
	productImageUrl := antHillData.Products[0].Product.Albums[0].Images[0].Src

	// Store product-sku in Neo4J customer.products.suggested

	// Send to Twilio
	twilioSmsResponse, err := util.SuggestionToSMS(phoneNumber, productImageUrl)
	if err != nil {
		return responses.TwilioSmsResponse{}, err
	}

	return twilioSmsResponse, nil
}

func GetSuggestion(c echo.Context) error {
	customerId := c.Param("id")
	channel := c.QueryParam("channel")
	phoneNumber := c.QueryParam("phone")

	queryResponse, queryError := util.AntHillQuery(customerId, channel)
	if queryError != nil {
		return c.String(http.StatusBadRequest, queryError.Error())
	}

	upSellResponse, upSellError := selectUpSellAndPushToSms(phoneNumber, queryResponse)
	if upSellError != nil {
		return c.String(http.StatusBadRequest, upSellError.Error())
	}

	encodedResponse, encodeErr := json.Marshal(&upSellResponse)
	if encodeErr != nil {
		return c.String(http.StatusBadRequest, encodeErr.Error())
	}

	return c.String(http.StatusOK, string(encodedResponse))
}

func TestNeo4j(c echo.Context) error {
	result, connErr := util.ConnectToNeo4J()
	if connErr != nil {
		return c.String(http.StatusBadRequest, connErr.Error())
	}

	return c.String(http.StatusOK, result)
}
