package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"io"
	"io/ioutil"
	"os"
	"os/exec"
	"strings"
)

var (
	// environmentsToGce maps the known universe
	environmentsToGce = map[string]string{
		"production": "",
		"staging":    "foxcomm-staging",
	}

	// project is the chosen GCE project
	project string

	// env is the given env arg
	env string

	// host filters based on tags (for ansible)
	host string

	// format will print for ansible when true otherwise the GCE response
	list bool
)

func init() {
	flag.StringVar(&env, "env", "", "Specify gcloud --project (environment). Required.")
	flag.StringVar(&host, "host", "", "Filters hosts based on GCE tags.")
	flag.BoolVar(&list, "list", true, "list for ansible when true otherwise print the GCE response.")
}

type Instances []Instance

type Instance struct {
	ID                string
	Kind              string
	Name              string
	Status            string
	MachineType       string
	NetworkInterfaces []NetworkInterface
	TagItems          struct {
		Items []string
	} `json:"tags"`
	Zone string
}

type NetworkInterface struct {
	Name          string
	Network       string
	IP            string `json:"networkIP"`
	AccessConfigs []AccessConfig
}

type AccessConfig struct {
	NatIP string
}

// PublicIP returns the NAT/publically accessible IP for this instance
func (i Instance) PublicIP() string {
	if len(i.NetworkInterfaces) > 0 && len(i.NetworkInterfaces[0].AccessConfigs) > 0 {
		return i.NetworkInterfaces[0].AccessConfigs[0].NatIP
	} else {
		return ""
	}
}

// PrivateIP returns the internal/private IP for this instance
func (i Instance) PrivateIP() string {
	if len(i.NetworkInterfaces) > 0 {
		return i.NetworkInterfaces[0].IP
	} else {
		return ""
	}
}

func (i Instance) Tags() []string {
	return i.TagItems.Items
}

// IsActive is it RUNNING
func (i Instance) IsActive() bool {
	return strings.ToUpper(i.Status) == "RUNNING"
}

func (i Instances) ToJSON() ([]byte, error) {
	capRoles := map[string]interface{}{}
	ansibleHostVars := map[string]map[string]string{}

	for _, instance := range i {
		ansibleHostVars[instance.PrivateIP()] = map[string]string{
			"private_ip_address": instance.PrivateIP(),
			"public_ip_address":  instance.PublicIP(),
			"ansible_node_name":  instance.Name,
		}

		for _, role := range instance.Tags() {
			if _, ok := capRoles[role]; !ok {
				capRoles[role] = []string{}
			}

			capRoles[role] = append(capRoles[role].([]string), instance.PrivateIP())
		}
	}

	output := capRoles
	output["_meta"] = map[string]interface{}{"hostvars": ansibleHostVars}

	return json.Marshal(output)
}

func Fetch() (io.ReadCloser, error) {
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

func List() (Instances, error) {
	reader, err := Fetch()

	if err != nil {
		return nil, err
	}

	return decode(reader)
}

func decode(r io.Reader) (Instances, error) {
	instances := Instances{}
	decoder := json.NewDecoder(r)
	if err := decoder.Decode(&instances); err != nil {
		return nil, err
	}

	running := make(Instances, 0, len(instances))
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
	flag.Parse()
	var ok bool

	if project, ok = environmentsToGce[env]; !ok {
		exit(1, fmt.Errorf("could not find env '%s'", env))
	}

	if !list {
		reader, err := Fetch()

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

	var result []byte
	var err error

	instances, err := List()
	if err != nil {
		exit(1, err)
	}

	if host != "" {
		filtered := make(Instances, 0, 0)

		for _, i := range instances {
			for _, tag := range i.Tags() {
				if tag == host {
					filtered = append(filtered, i)
				}
			}
		}

		instances = filtered
	}

	result, err = instances.ToJSON()
	if err != nil {
		exit(1, err)
	}

	fmt.Println(string(result))
}
