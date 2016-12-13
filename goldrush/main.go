package main

import (
	"os"

	"github.com/urfave/cli"
)

func main() {

	app := cli.NewApp()
	app.Name = Name
	app.Version = Version
	app.Author = "foxcomm"
	app.Email = ""
	app.Usage = "FoxCommerce developer toolkit that simplifies the process of bringing up the environment locally and deploying to it."

	app.Flags = GlobalFlags
	app.Commands = Commands
	app.CommandNotFound = CommandNotFound

	app.Run(os.Args)
}
