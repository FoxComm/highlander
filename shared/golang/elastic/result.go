package elastic

type Result struct {
	Took     int  `json:"took"`
	TimedOut bool `json:"timed_out"`
	Hits     Hits `json:"hits"`
}

func (r Result) ExtractHits() []Hit {
	return r.Hits.Hits
}
