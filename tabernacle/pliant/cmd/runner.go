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

	if r.OptionAll {
		return r.PullAll()
	} else if r.OptionIndex != "" {
		return r.PullForIndex(r.OptionIndex)
	} else if r.OptionMapping != "" {
		return r.PullMapping(r.OptionMapping)
	}

	return errors.New("Must select one of --all, --index, or --mapping.")
}

const (
	infoPullAll     = "\nPull all mappings from cluster %s...\n\n"
	infoPullIndex   = "\nPull mappings in index %s for cluster %s...\n\n"
	infoPullMapping = "\nPulling mapping %s from cluster %s...\n\n"

	infoMappingFoundInIndex = "Found mapping in index %s...\n"
)

func (r *Runner) PullAll() error {
	fmt.Printf(infoPullAll, r.cfg.ElasticURL())

	mappings, err := r.client.GetMappingsInCluster()
	if err != nil {
		return err
	}

	return r.writeMappings(mappings)
}

func (r *Runner) PullForIndex(index string) error {
	fmt.Printf(infoPullIndex, index, r.cfg.ElasticURL())

	mappings, err := r.client.GetMappingsInIndex(index)
	if err != nil {
		return err
	}

	return r.writeMappings(mappings)
}

func (r *Runner) PullMapping(mappingName string) error {
	fmt.Printf(infoPullMapping, mappingName, r.cfg.ElasticURL())

	searchDefn, err := r.cfg.SearchDefinitionByMapping(mappingName)
	if err != nil {
		return err
	}

	fmt.Printf(infoMappingFoundInIndex, searchDefn.Index)
	mapping, err := r.client.GetMapping(searchDefn.Index, mappingName)
	if err != nil {
		return err
	}

	mappings := map[string]elastic.Mapping{mappingName: *mapping}
	return r.writeMappings(mappings)
}

func (r *Runner) writeMappings(mappings map[string]elastic.Mapping) error {
	for name, mapping := range mappings {
		filename := fmt.Sprintf("./mappings/%s.json", name)
		fmt.Printf("Writing %s...\n", filename)

		bytes, err := json.Marshal(mapping)
		if err != nil {
			return err
		}

		if err := ioutil.WriteFile(filename, bytes, 0644); err != nil {
			return err
		}
	}

	return nil
}
