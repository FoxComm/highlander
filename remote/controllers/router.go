package controllers

import (
	"fmt"

	"github.com/labstack/echo"
)

type RouterFunc func(fc *FoxContext) error

type Router struct {
	e *echo.Echo
}

func NewRouter() *Router {
	e := echo.New()

	e.Use(func(h echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			fc := NewFoxContext(c)
			return h(fc)
		}
	})

	return &Router{e}
}

func (r *Router) Run(port int) {
	r.e.Start(fmt.Sprintf(":%d", port))
}

func (r *Router) GET(uri string, rf RouterFunc) {
	r.e.GET(uri, func(c echo.Context) error {
		fc := c.(*FoxContext)
		return rf(fc)
	})
}

func (r *Router) POST(uri string, rf RouterFunc) {
	r.e.POST(uri, func(c echo.Context) error {
		fc := c.(*FoxContext)
		return rf(fc)
	})
}
