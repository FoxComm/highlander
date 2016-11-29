package phoenix

import (
	"encoding/json"

	"github.com/FoxComm/highlander/middlewarehouse/consumers/capture/lib"
)

type OrderBulkStateChange struct {
	CordRefNums []string `json:"cordRefNums" binding:"required"`
	NewState    string   `json:"newState" binding:"required"`
}

func NewOrderBulkStateChangeFromActivity(activity *Activity) (*OrderBulkStateChange, error) {
	bt := []byte(activity.Data)
	fo := new(OrderBulkStateChange)
	err := json.Unmarshal(bt, fo)
	return fo, err
}

func (ac *OrderBulkStateChange) GetRelatedOrders(client lib.PhoenixClient) ([]*FullOrder, error) {
	orders := []*FullOrder{}
	for _, refNum := range ac.CordRefNums {
		payload, err := client.GetOrder(refNum)
		if err != nil {
			return orders, err
		}

		fullOrder := NewFullOrderFromPayload(payload)
		orders = append(orders, fullOrder)
	}

	return orders, nil
}
