package gcloud

import (
	"encoding/json"
	"strings"
)

const (
	STATUS_RUNNING    = "RUNNING"
	STATUS_TERMINATED = "TERMINATED"
)

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
	return strings.ToUpper(i.Status) == STATUS_RUNNING
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
