package responses

type Error struct {
	Errors []interface{} `json:"errors"`
}
