package phoenix

type Activity struct {
	Type string `json:"activity_type" binding:"required"`
	Data string `json:"data"`
}
