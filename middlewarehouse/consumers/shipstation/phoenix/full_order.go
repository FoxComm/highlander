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
	fo := new(FullOrder)

	defer response.Body.Close()

	orderResp := new(Order)
	if err := json.NewDecoder(response.Body).Decode(orderResp); err != nil {
		log.Printf("Unable to read order response from Phoenix with error: %s", err.Error())
		return nil, err
	}

	fo.Order = *orderResp
	return fo, nil
}
