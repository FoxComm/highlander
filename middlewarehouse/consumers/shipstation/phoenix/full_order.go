package phoenix

import (
	"encoding/json"
	"log"
	"net/http"
)

type FullOrder struct {
	Order Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity *Activity) (*FullOrder, error) {
	bt := []byte(activity.Data)
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, err
}

func NewFullOrderFromHttpResponse(response *http.Response) (*FullOrder, error) {
	defer response.Body.Close()

	orderResult := new(OrderResult)
	if err := json.NewDecoder(response.Body).Decode(orderResp); err != nil {
		log.Printf("Unable to read order response from Phoenix with error: %s", err.Error())
		return nil, err
	}

	fo := new(FullOrder)
	fo.Order = orderResult.Order
	return fo, nil
}
