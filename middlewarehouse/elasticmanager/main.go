package main

import (
	"os"

	"github.com/FoxComm/highlander/middlewarehouse/elasticmanager/cmd"
	"github.com/urfave/cli"
)

func main() {
	runner := cmd.NewRunner()

	app := cli.NewApp()

	app.Name = "elasticmanager"
	app.Usage = "manage indices, aliases, and mappings in ElasticSearch"
	app.Version = "0.0.1"

	app.Commands = []cli.Command{
		{
			Name:    "create",
			Aliases: []string{"c"},
			Usage:   "create one or more new searches",
			Flags: []cli.Flag{
				cli.BoolFlag{
					Name:        "all, a",
					Usage:       "create all searches",
					Destination: &runner.OptionAll,
				},
				cli.StringFlag{
					Name:        "search, s",
					Usage:       "creates the search `SEARCH`",
					Destination: &runner.OptionSearch,
				},
			},
			Action: runner.Create,
		},
		{
			Name:    "update",
			Aliases: []string{"u"},
			Usage:   "update one or more searches",
			Flags: []cli.Flag{
				cli.BoolFlag{
					Name:        "all, a",
					Usage:       "update all searches",
					Destination: &runner.OptionAll,
				},
				cli.StringFlag{
					Name:        "search, s",
					Usage:       "updates the search `SEARCH`",
					Destination: &runner.OptionSearch,
				},
			},
			Action: runner.Update,
		},
		{
			Name:    "pull",
			Aliases: []string{"p"},
			Usage:   "pull mappings from the ES cluster",
			Flags: []cli.Flag{
				cli.BoolFlag{
					Name:        "all, a",
					Usage:       "pull all indices and mappings",
					Destination: &runner.OptionAll,
				},
				cli.StringFlag{
					Name:        "index, i",
					Usage:       "pull all mappings in `INDEX`",
					Destination: &runner.OptionIndex,
				},
				cli.StringFlag{
					Name:        "mapping, m",
					Usage:       "pull a single `MAPPING`",
					Destination: &runner.OptionMapping,
				},
			},
			Action: runner.Pull,
		},
	}

	app.Run(os.Args)
}
