wrk.method = "POST"
wrk.body = "{\"query\":{\"bool\":{\"filter\":[{\"missing\":{\"field\":\"archivedAt\"}}]}}}" 
wrk.headers["Content-Type"] = "application/json"
