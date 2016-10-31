package api

type Context struct {
	Name       string            `json:"name"`
	Attributes map[string]string `json:"attributes"`
}
