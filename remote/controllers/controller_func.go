package controllers

import "github.com/FoxComm/highlander/remote/responses"

// ControllerFunc is the execution API that any function called in a route must
// conform to. It executes, then returns a response that can be handled.
type ControllerFunc func() *responses.Response
