package services

import (
	"encoding/json"
	"errors"
	"net/http"

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
	"github.com/FoxComm/highlander/intelligence/suggester/src/util"
	"github.com/labstack/echo"
)

func selectUpSellAndPushToSms(customerID string, phoneNumber string, antHillData responses.AntHillResponse) (responses.TwilioSmsResponse, error) {
	if len(antHillData.Products) < 1 {
		return responses.TwilioSmsResponse{}, errors.New("There are no products for potential up-sell")
	}

	// TODO: After testing create a selection algorithm based on score or previous Rejections filter out
	productImageURL := antHillData.Products[0].Product.Albums[0].Images[0].Src
	productID := antHillData.Products[0].Product.Id

	// Create customer <- Suggest -> product in Neo4J
	_, err := util.CreateNewSuggestProductRelation(customerID, string(productID))
	if err != nil {
		return responses.TwilioSmsResponse{}, err
	}

	// Send to Twilio
	twilioSmsResponse, err := util.SuggestionToSMS(phoneNumber, productImageURL)
	if err != nil {
		return responses.TwilioSmsResponse{}, err
	}

	return twilioSmsResponse, nil
}

func GetSuggestion(c echo.Context) error {
	customerID := c.Param("id")
	channel := c.QueryParam("channel")
	phoneNumber := c.QueryParam("phone")

	queryResponse, queryError := util.AntHillQuery(customerID, channel)
	if queryError != nil {
		return c.String(http.StatusBadRequest, queryError.Error())
	}

	upSellResponse, upSellError := selectUpSellAndPushToSms(customerID, phoneNumber, queryResponse)
	if upSellError != nil {
		return c.String(http.StatusBadRequest, upSellError.Error())
	}

	encodedResponse, encodeErr := json.Marshal(&upSellResponse)
	if encodeErr != nil {
		return c.String(http.StatusBadRequest, encodeErr.Error())
	}

	return c.String(http.StatusOK, string(encodedResponse))
}

func DeclineSuggestion(c echo.Context) error {
	//phoneNumber := c.Param("phone")

	return c.String(http.StatusOK, "")
}

func PurchaseSuggestion(c echo.Context) error {
	//phoneNumber := c.Param("phone")

	return c.String(http.StatusOK, "")
}
