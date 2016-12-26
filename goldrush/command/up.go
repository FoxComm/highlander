package command

import (
	"github.com/urfave/cli"
	"fmt"
)


func CmdUp(c *cli.Context) {
	// Write some code here
	fmt.Fprintf(c.App.Writer, "Deploying and provisioning the Fox environment...")
}
