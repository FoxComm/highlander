package responses

type PluginResponse struct {
	Name        string `json:"name"`
	Description string `json:"description"`
	Version     string `json:"version"`
	CreatedAt   string `json:"createdAt"`
}

type PluginSettingsResponse map[string]string
