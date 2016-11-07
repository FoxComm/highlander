package elastic

type Hit struct {
	Index  string      `json:"_index"`
	Type   string      `json:"_type"`
	ID     string      `json:"_id"`
	Score  int         `json:"_score"`
	Source interface{} `json:"_source"`
}
