package responses

type HenhouseStats struct {
	Variance   float32 `json:"variance"`
	Mean       float32 `json:"mean"`
	From       int     `json:"from"`
	To         int     `json:"to"`
	Resolution int     `json:"resolution"`
	Points     int     `json:"points"`
	Sum        int     `json:"sum"`
}

type HenhouseResponse []struct {
	Key   string        `json:"key"`
	Stats HenhouseStats `json:"stats"`
}