package cmd

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"

	"github.com/FoxComm/highlander/tabernacle/pliant/elastic"
	"github.com/urfave/cli"
)

const defaultConfig = "config.yml"

type Runner struct {
	OptionAll     bool
	OptionIndex   string
	OptionMapping string
	OptionSearch  string
	cfg           *Config
	client        *elastic.Client
}

func NewRunner() *Runner {
	return &Runner{}
}

func (r *Runner) initialize() error {
	var err error

	if r.cfg, err = NewConfig(defaultConfig); err != nil {
		return err
	}

	r.client = elastic.NewClient(r.cfg.ElasticURL())
	if err = r.client.Connect(); err != nil {
		return err
	}

	return nil
}

func (r *Runner) Create(c *cli.Context) error {
	if err := r.initialize(); err != nil {
		fmt.Printf("Error: %s\n", err.Error())
		return err
	}

	if r.OptionAll {
		fmt.Println("Creating all...")
	} else {
		fmt.Println("Creating one...")
	}

	return nil
}

func (r *Runner) Update(c *cli.Context) error {
	if err := r.initialize(); err != nil {
		return err
	}

	if r.OptionAll {
		fmt.Println("Updating all...")
	} else {
		fmt.Println("Updating one...")
	}

	return nil
}

func (r *Runner) Pull(c *cli.Context) error {
	if err := r.initialize(); err != nil {
		return err
	}

	var err error
	var mappings map[string]elastic.Mapping

	if r.OptionAll {
		fmt.Printf("\nPulling all mappings from cluster %s...\n\n", r.cfg.ElasticURL())
		mappings, err = r.client.GetMappingsInCluster()
	} else if r.OptionIndex != "" {
		fmt.Printf("\nPulling mappings in index %s from cluster %s...\n\n", r.OptionIndex, r.cfg.ElasticURL())
		mappings, err = r.client.GetMappingsInIndex(r.OptionIndex)
	} else if r.OptionMapping != "" {
		fmt.Printf("\nPulling mapping %s from cluster %s...\n\n", r.OptionMapping, r.cfg.ElasticURL())

		return errors.New("Not implemented...")
	} else {
		return errors.New("Must select one of --all, --index, or --mapping.")
	}

	if err != nil {
		return err
	}

	for name, mapping := range mappings {
		if err := r.writeMapping(name, mapping); err != nil {
			return nil
		}
	}

	return nil
}

func (r *Runner) writeMapping(name string, contents elastic.Mapping) error {
	filename := fmt.Sprintf("./mappings/%s.json", name)
	fmt.Printf("Writing %s...\n", filename)

	bytes, err := json.Marshal(contents)
	if err != nil {
		return err
	}

	if err := ioutil.WriteFile(filename, bytes, 0644); err != nil {
		return err
	}

	return nil
}
