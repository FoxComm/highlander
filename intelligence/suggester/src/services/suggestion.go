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
	// Query Neo4J for products that this customer has either been Suggested, Purchased and or Declined
	neo4jResponse, findSuggestedErr := util.FindAllProductsSuggestedForCustomer(customerID)
	if findSuggestedErr != nil {
		return responses.ProductInstance{}, findSuggestedErr
	}

	// Build map of suggested, purchased and or declined product phoenix_id(s)
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

func selectUpSellAndPushToSmsDexter(customerID string, phoneNumber string, antHillData responses.AntHillResponse) (responses.RunDexterResponse, error) {
	if len(antHillData.Products) < 1 {
		return responses.RunDexterResponse{}, errors.New("There are no products for potential up-sell")
	}

	// Determine up-sell product and gather image URL, ID number and SKU
	var upSellProduct responses.ProductInstance

	upSellProduct, determineUpsellErr := determineUpSellProductFromAnthillData(customerID, antHillData)
	if determineUpsellErr != nil {
		fmt.Println("UpSell Error = ", determineUpsellErr)
		upSellProduct = antHillData.Products[0].Product
	}

	productID := upSellProduct.ProductId
	productSKU := upSellProduct.Skus[0]

	// Create customer <- Suggest -> product in Neo4J
	_, err := util.CreateNewSuggestProductRelation(customerID, strconv.Itoa(productID), phoneNumber, productSKU)
	if err != nil {
		return responses.RunDexterResponse{}, err
	}

	// Send to Dexter
	runDexterSmsResponse, err := util.DexterSuggestionToSMS(phoneNumber, "upsell", upSellProduct)
	if err != nil {
		return responses.RunDexterResponse{}, err
	}

	return runDexterSmsResponse, nil
}

func normalizePhoneNumber(phoneNumber string) (string, error) {
	if len(phoneNumber) <= 1 {
		return phoneNumber, errors.New("PhoneNumber missing")
	}

	phoneNumber = strings.Replace(phoneNumber, " ", "", -1)

	// Check for international code
	if phoneNumber[0] != '+' {
		if phoneNumber[0] != '1' {
			phoneNumber = "1" + phoneNumber
		}
		phoneNumber = "+" + phoneNumber
	}

	return phoneNumber, nil
}

func GetSuggestion(c echo.Context) error {
	channel := c.QueryParam("channel")

	customer := new(payloads.Customer)
	payloadError := c.Bind(customer)
	if payloadError != nil {
		return c.String(http.StatusBadRequest, "payloadError: "+payloadError.Error())
	}

	customerID := customer.CustomerID

	anthillQueryResponse, queryError := util.AntHillQuery(customerID, channel)
	if queryError != nil {
		return c.String(http.StatusBadRequest, "Anthill query response error: "+queryError.Error())
	}

	phoneNumberClean, phoneError := normalizePhoneNumber(customer.PhoneNumber)
	if phoneError != nil {
		return c.String(http.StatusBadRequest, "phoneNumber error: "+phoneError.Error())
	}

	upSellResponse, upSellError := selectUpSellAndPushToSmsDexter(customerID, phoneNumberClean, anthillQueryResponse)
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
	customerID, _, productSKU, lookupErr := util.FindCustomerAndProductFromPhoneNumber(phoneNumber)
	if lookupErr != nil {
		return c.String(http.StatusBadRequest, lookupErr.Error())
	}

	purchaseResp, purchaseErr := util.OneClickPurchase(customerID, productSKU)
	if purchaseErr != nil {
		return c.String(http.StatusBadRequest, purchaseErr.Error())
	}

	return c.String(http.StatusOK, purchaseResp)
}
