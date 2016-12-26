package main

import (
	"fmt"
	"os"

	"github.com/foxcomm/highlander/goldrush/command"
	"github.com/urfave/cli"
)

var GlobalFlags = []cli.Flag{}

var Commands = []cli.Command{
	{
		Name:   "info",
		Usage:  "Tells you what is currently running in your environment.",
		Action: command.CmdInfo,
		Flags:  []cli.Flag{},
	},
	{
		Name:   "provision",
		Usage:  "Provisions environment for you.",
		Action: command.CmdProvision,
		Flags: []cli.Flag{
			cli.BoolTFlag{Name: "vmware"},
			cli.BoolTFlag{Name: "virtualbox"},
		},
	},
	{
		Name:   "deploy",
		Usage:  "Let's you deploy directly to one or more of the projects.",
		Action: command.CmdDeploy,
		Flags:  []cli.Flag{},
	},
	{
		Name:   "up",
		Usage:  "Deploys and provisions the environment for you.",
		Action: command.CmdUp,
		Flags: []cli.Flag{
			cli.BoolTFlag{Name: "vmware"},
			cli.BoolTFlag{Name: "virtualbox"},
		},
	},
	{
		Name:   "phoenix",
		Usage:  "Specific phoenix subcommands.",
		Action: command.CmdPhoenix,
		Flags:  []cli.Flag{},
	},
}

func CommandNotFound(c *cli.Context, command string) {
	fmt.Fprintf(os.Stderr, "%s: '%s' is not a %s command. See '%s --help'.", c.App.Name, command, c.App.Name, c.App.Name)
	os.Exit(2)
}
