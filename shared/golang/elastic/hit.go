package elastic

import "encoding/json"

type Hit struct {
	Index  string      `json:"_index"`
	Type   string      `json:"_type"`
	ID     string      `json:"_id"`
	Score  float64     `json:"_score"`
	Source interface{} `json:"_source"`
}

func (h Hit) Extract(val interface{}) error {
	bytes, err := json.Marshal(h.Source)
	if err != nil {
		return err
	}

	if err := json.Unmarshal(bytes, val); err != nil {
		return err
	}

	return nil
}
