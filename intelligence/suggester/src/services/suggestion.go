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

	// TODO: After testing create a selection algorithm based on score
	productImageUrl := antHillData.Products[0].Product.Albums[0].Images[0].Src
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
