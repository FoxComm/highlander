package main

import (
	"./gcloud"
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"os/exec"
)

const DEFAULT_MAPPING_FILE = "projects.json"

type Config struct {
	// Environment, mapped to gcloud project
	Environment string
	// Path to mapping file
	Mapping string
	// Host filters based on tags (for ansible)
	Host string
	// Will print for ansible when true, otherwise the GCE response
	List bool
}

func Fetch(project string) (io.ReadCloser, error) {
	cmd := exec.Command("gcloud", "compute", "instances", "list", "--format", "json", "--project", project)
	out, err := cmd.StdoutPipe()
	if err != nil {
		return nil, err
	}

	if err = cmd.Start(); err != nil {
		return nil, err
	}

	return out, nil
}

func List(project string) (gcloud.Instances, error) {
	reader, err := Fetch(project)

	if err != nil {
		return nil, err
	}

	return decode(reader)
}

func decode(r io.Reader) (gcloud.Instances, error) {
	instances := gcloud.Instances{}
	decoder := json.NewDecoder(r)
	if err := decoder.Decode(&instances); err != nil {
		return nil, err
	}

	running := make(gcloud.Instances, 0, len(instances))
	for _, inst := range instances {
		if inst.IsActive() {
			running = append(running, inst)
		}
	}
	return running, nil
}

func exit(status int, err error) {
	fmt.Println(err.Error(), "\n")
	flag.PrintDefaults()
	os.Exit(status)
}

func main() {
	// Parse command-line flags into config structure
	config := Config{}
	flag.StringVar(&config.Environment, "env", "", "Specify gcloud --project (environment). Required.")
	flag.StringVar(&config.Mapping, "map", DEFAULT_MAPPING_FILE, "Specify projects to gce mapping file.")
	flag.StringVar(&config.Host, "host", "", "Filters hosts based on GCE tags.")
	flag.BoolVar(&config.List, "list", true, "list for ansible when true otherwise print the GCE response.")
	flag.Parse()

	// Read mapping file
	bytes, err := ioutil.ReadFile(config.Mapping)
	if err != nil {
		exit(1, err)
	}

	// Unmarshal mapping to map
	environmentsToGce := map[string]string{}
	err = json.Unmarshal(bytes, &environmentsToGce)
	if err != nil {
		exit(1, err)
	}

	// Find requested project
	var project string
	var ok bool
	if project, ok = environmentsToGce[config.Environment]; !ok {
		exit(1, fmt.Errorf("Could not find environment '%s'", config.Environment))
	}

	// Main command
	if !config.List {
		reader, err := Fetch(project)

		if err != nil {
			exit(1, err)
		}

		defer reader.Close()
		data, err := ioutil.ReadAll(reader)
		if err != nil {
			exit(1, err)
		}

		fmt.Println(string(data))
		return
	}

	// List command
	instances, err := List(project)
	if err != nil {
		exit(1, err)
	}

	if config.Host != "" {
		filtered := make(gcloud.Instances, 0, 0)

		for _, i := range instances {
			for _, tag := range i.Tags() {
				if tag == config.Host {
					filtered = append(filtered, i)
				}
			}
		}

		instances = filtered
	}

	result, err := instances.ToJSON()
	if err != nil {
		exit(1, err)
	}

	fmt.Println(string(result))
}
