package controllers

import "strings"

func validateRoutePrefix(routePrefix string) error {
	if routePrefix == "" {
		return newEmptyRoutePrefixError()
	} else if !strings.HasPrefix(routePrefix, "/") {
		return newBeginningRoutePrefixError(routePrefix)
	} else if strings.HasSuffix(routePrefix, "/") {
		return newEndingRoutePrefixError(routePrefix)
	}

	return nil
}
