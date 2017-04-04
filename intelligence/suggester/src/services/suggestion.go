package services

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
	"github.com/FoxComm/highlander/intelligence/suggester/src/util"
	"github.com/labstack/echo"
)

func selectUpSellAndPushToSms(customerID string, phoneNumber string, antHillData responses.AntHillResponse) (responses.TwilioSmsResponse, error) {
	if len(antHillData.Products) < 1 {
		return responses.TwilioSmsResponse{}, errors.New("There are no products for potential up-sell")
	}

	// The first item in the cross-sell results will have the highest reccomendation score
	productImageURL := antHillData.Products[0].Product.Albums[0].Images[0].Src
	productID := antHillData.Products[0].Product.ProductId
	productSKU := antHillData.Products[0].Product.Skus[0]

	// Create customer <- Suggest -> product in Neo4J
	_, err := util.CreateNewSuggestProductRelation(customerID, strconv.Itoa(productID), phoneNumber, productSKU)
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
	phoneNumberClean := "+" + strings.Replace(phoneNumber, " ", "", -1)

	queryResponse, queryError := util.AntHillQuery(customerID, channel)
	if queryError != nil {
		return c.String(http.StatusBadRequest, queryError.Error())
	}

	upSellResponse, upSellError := selectUpSellAndPushToSms(customerID, phoneNumberClean, queryResponse)
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
	phoneNumber := c.Param("phone")
	customerID, productID, productSKU, lookupErr := util.FindCustomerAndProductFromPhoneNumber(phoneNumber)
	if lookupErr != nil {
		return c.String(http.StatusBadRequest, lookupErr.Error())
	}

	_, declineErr := util.CreateNewDeclinedProductRelation(customerID, productID)
	if declineErr != nil {
		return c.String(http.StatusBadRequest, declineErr.Error())
	}

	respMsg := fmt.Sprintf("Customer %s declined Product %s with SKU %s", customerID, productID, productSKU)
	return c.String(http.StatusOK, respMsg)
}

func PurchaseSuggestion(c echo.Context) error {
	//phoneNumber := c.Param("phone")
	//customerID, productID, _, lookupErr := util.FindCustomerAndProductFromPhoneNumber(phoneNumber)
	//if lookupErr != nil {
	//return c.String(http.StatusBadRequest, lookupErr.Error())
	//}

	return c.String(http.StatusOK, "")
}
