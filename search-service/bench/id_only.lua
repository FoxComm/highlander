wrk.method = "POST"
wrk.body = "{\"query\":{\"bool\":{\"filter\":[{\"missing\":{\"field\":\"archivedAt\"}}]}}, \"fields\":[\"id\"]}" 
wrk.headers["Content-Type"] = "application/json"
