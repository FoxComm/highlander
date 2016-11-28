package phoenix

import "encoding/json"

type FullOrder struct {
	Order Order `json:"Order" binding:"required"`
}

func NewFullOrderFromActivity(activity *Activity) (*FullOrder, error) {
	bt := []byte(activity.Data)
	fo := new(FullOrder)
	err := json.Unmarshal(bt, fo)
	return fo, err
}
