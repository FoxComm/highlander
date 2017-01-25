package responses

import (
	"strings"
)

type HenhouseStats struct {
	Variance   float64 `json:"variance"`
	Mean       float64 `json:"mean"`
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

func GetSum(step string, pf HenhouseResponse) int {
	for i := range pf {
		if strings.Contains(pf[i].Key, step) {
			return pf[i].Stats.Sum
		}
	}
	return 0
}
