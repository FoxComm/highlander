package services

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/FoxComm/highlander/intelligence/suggester/src/payloads"
	"github.com/FoxComm/highlander/intelligence/suggester/src/responses"
	"github.com/FoxComm/highlander/intelligence/suggester/src/util"

	"github.com/labstack/echo"
)

func determineUpSellProductFromAnthillData(customerID string, antHillData responses.AntHillResponse) (responses.ProductInstance, error) {
	// Query Neo4J for suggested products
	queryResp, findSuggestedErr := util.FindAllProductsSuggestedForCustomer(customerID)
	if findSuggestedErr != nil {
		return responses.ProductInstance{}, findSuggestedErr
	}

	// Parse response into suggested products array
	var neo4jResponse responses.Neo4jResponse
	queryRespBytes := []byte(queryResp)
	jsonErr := json.Unmarshal(queryRespBytes, &neo4jResponse)
	if jsonErr != nil {
		return responses.ProductInstance{}, jsonErr
	}

	// Build map of suggested product phoenix_id
	if len(neo4jResponse.Results) <= 0 {
		return responses.ProductInstance{}, errors.New("Customer has no previously suggested products")
	}
	suggestedProductDataNodes := neo4jResponse.Results[0].Data
	suggestedProductsFromNeoMap := make(map[int]bool)

	for dataNodeIdx := 0; dataNodeIdx < len(suggestedProductDataNodes); dataNodeIdx++ {
		productID := suggestedProductDataNodes[dataNodeIdx].Graph.Nodes[0].Properties.PhoenixID
		suggestedProductsFromNeoMap[productID] = true
	}

	// Find the first anthill product that is not in the suggestedProductsFromNeoMap
	for antHillIdx := 0; antHillIdx < len(antHillData.Products); antHillIdx++ {
		antHillProdID := antHillData.Products[antHillIdx].Product.ProductId
		_, productFound := suggestedProductsFromNeoMap[antHillProdID]

		if productFound {
			continue
		} else {
			return antHillData.Products[antHillIdx].Product, nil
		}
	}

	return responses.ProductInstance{}, errors.New("determineUpSellProductFromAnthillData nothing to suggest")
}

func selectUpSellAndPushToSms(customerID string, phoneNumber string, antHillData responses.AntHillResponse) (responses.TwilioSmsResponse, error) {
	if len(antHillData.Products) < 1 {
		return responses.TwilioSmsResponse{}, errors.New("There are no products for potential up-sell")
	}

	// Determine up-sell product and gather image URL, ID number and SKU
	upSellProduct, determineUpsellErr := determineUpSellProductFromAnthillData(customerID, antHillData)
	if determineUpsellErr != nil {
		return responses.TwilioSmsResponse{}, errors.New("Every possible up-sell product has been suggested already")
	}

	productImageURL := upSellProduct.Albums[0].Images[0].Src
	productID := upSellProduct.ProductId
	productSKU := upSellProduct.Skus[0]

	// Create customer <- Suggest -> product in Neo4J
	_, err := util.CreateNewSuggestProductRelation(customerID, strconv.Itoa(productID), phoneNumber, productSKU)
	if err != nil {
		return responses.TwilioSmsResponse{}, err
	}

	// Send to Twilio
	twilioSmsResponse, err := util.SuggestionToSMS(phoneNumber, productImageURL, upSellProduct)
	if err != nil {
		return responses.TwilioSmsResponse{}, err
	}

	return twilioSmsResponse, nil
}

func GetSuggestion(c echo.Context) error {
	channel := c.QueryParam("channel")

	customer := new(payloads.Customer)
	payloadError := c.Bind(customer)
	if payloadError != nil {
		return c.String(http.StatusBadRequest, "payloadError: "+payloadError.Error())
	}

	customerID := customer.CustomerID
	phoneNumberClean := "+" + strings.Replace(customer.PhoneNumber, " ", "", -1)

	queryResponse, queryError := util.AntHillQuery(customerID, channel)
	if queryError != nil {
		return c.String(http.StatusBadRequest, "queryError: "+queryError.Error())
	}

	upSellResponse, upSellError := selectUpSellAndPushToSms(customerID, phoneNumberClean, queryResponse)
	if upSellError != nil {
		return c.String(http.StatusBadRequest, "upSellError: "+upSellError.Error())
	}

	encodedResponse, encodeErr := json.Marshal(&upSellResponse)
	if encodeErr != nil {
		return c.String(http.StatusBadRequest, "encodedError: "+encodeErr.Error())
	}

	return c.String(http.StatusOK, string(encodedResponse))
}

func DeclineSuggestion(c echo.Context) error {
	phoneNumber := c.Param("phoneNumber")
	customerID, productID, productSKU, lookupErr := util.FindCustomerAndProductFromPhoneNumber(phoneNumber)
	if lookupErr != nil {
		return c.String(http.StatusBadRequest, lookupErr.Error())
	}

	_, declineErr := util.CreateNewDeclinedProductRelation(customerID, productID)
	if declineErr != nil {
		return c.String(http.StatusBadRequest, declineErr.Error())
	}

	respMsg := fmt.Sprintf("Customer %s declined Product %s with SKU %s", customerID, productID, productSKU)
	responseMap := map[string]string{"message": respMsg, "customerID": customerID, "productID": productID, "productSKU": productSKU}
	responseJSON, responseJSONErr := json.Marshal(responseMap)
	if responseJSONErr != nil {
		return c.String(http.StatusBadRequest, responseJSONErr.Error())
	}

	return c.String(http.StatusOK, string(responseJSON))
}

func PurchaseSuggestion(c echo.Context) error {
	phoneNumber := c.Param("phoneNumber")
	customerID, productID, productSKU, lookupErr := util.FindCustomerAndProductFromPhoneNumber(phoneNumber)
	if lookupErr != nil {
		return c.String(http.StatusBadRequest, lookupErr.Error())
	}

	purchaseResp, purchaseErr := util.OneClickPurchase(customerID, productSKU)
	if purchaseErr != nil {
		return c.String(http.StatusBadRequest, purchaseErr.Error())
	}

	_, purchaseRelationErr := util.CreateNewPurchasedProductRelation(customerID, productID)
	if purchaseRelationErr != nil {
		return c.String(http.StatusBadRequest, purchaseRelationErr.Error())
	}

	return c.String(http.StatusOK, purchaseResp)
}
