package main

import (
	"./gcloud"
	"bytes"
	"testing"

	"github.com/stretchr/testify/assert"
)

var goodInstancesJSON = `
[{
	"name": "prod-bk-app-09",
	"id": "9867307197979489186",
	"kind": "compute#instance",
	"machineType": "f1-micro",
	"networkInterfaces": [{
		"name": "nic0",
		"network": "default",
		"networkIP": "10.240.149.74",
		"accessConfigs": [{
			"natIP": "8.8.8.8"
		}]
	}],
	"status": "RUNNING",
	"tags": {
		"fingerprint": "JFlJxLpjotc=",
		"items": [
			"mongodb"
		]
	},
	"zone": "us-central1-a"
}]
`

var badInstanceJSON = ""

func decodeString(str string) (gcloud.Instances, error) {
	reader := bytes.NewBufferString("")
	_, err := reader.WriteString(str)
	if err != nil {
		return nil, err
	}
	return decode(reader)
}

func TestDecode(t *testing.T) {
	instances, err := decodeString(goodInstancesJSON)

	assert.Len(t, instances, 1)
	assert.NoError(t, err)

	instances, err = decodeString(badInstanceJSON)

	assert.Len(t, instances, 0)
	assert.Error(t, err)
}

func TestIPs(t *testing.T) {
	instance := gcloud.Instance{
		NetworkInterfaces: []gcloud.NetworkInterface{
			gcloud.NetworkInterface{
				IP: "10.240.149.74",
				AccessConfigs: []gcloud.AccessConfig{
					gcloud.AccessConfig{
						NatIP: "8.8.8.8",
					},
				},
			},
		},
	}

	public := instance.PublicIP()
	private := instance.PrivateIP()

	assert.Equal(t, public, "8.8.8.8")
	assert.Equal(t, private, "10.240.149.74")
}

func TestToJSON(t *testing.T) {
	instances, _ := decodeString(goodInstancesJSON)
	bytes, err := instances.ToJSON()
	expected := `{"_meta":{"hostvars":{"10.240.149.74":{"ansible_node_name":"prod-bk-app-09","private_ip_address":"10.240.149.74","public_ip_address":"8.8.8.8"}}},"mongodb":["10.240.149.74"]}`

	assert.Len(t, instances, 1)
	assert.NoError(t, err)
	assert.Equal(t, expected, string(bytes))
}

func TestIsActive(t *testing.T) {
	assert.True(t, gcloud.Instance{Status: gcloud.STATUS_RUNNING}.IsActive())
	assert.False(t, gcloud.Instance{Status: gcloud.STATUS_TERMINATED}.IsActive())
}

func TestDecodeOnlyIncludesActive(t *testing.T) {
	jsonStr := `[
		{
			"name": "prod-bk-app-09",
			"status": "RUNNING"
		},
		{
			"name": "prod-core-01",
			"status": "TERMINATED"
		}
	]`

	instances, err := decodeString(jsonStr)

	assert.NoError(t, err)
	assert.Len(t, instances, 1)
	assert.Equal(t, "prod-bk-app-09", instances[0].Name)
}
