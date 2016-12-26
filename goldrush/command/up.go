package command

import (
	"github.com/urfave/cli"
	"fmt"
	"os/exec"
	"log"
	"bufio"
	"time"
	"github.com/fatih/color"
)


func CmdUp(c *cli.Context) {
	// Write some code here
	fmt.Fprintf(c.App.Writer, "Deploying and provisioning the Fox environment...")

	cmdName := "vagrant"
	cmdArgs := []string{"up"}

	if c.BoolT("vmware") {
		fmt.Printf("VMWARE!")
		cmdArgs = append(cmdArgs, "--provider=vmware_fusion")
	}

	cmd := exec.Command(cmdName, cmdArgs...)
	cmdReader, err := cmd.StdoutPipe()
	if err != nil {
		log.Fatal(err)
	}

	errReader, err := cmd.StderrPipe()
	if err != nil {
		log.Fatal(err)
	}

	fmt.Printf("Running Vagrant Up.. Output below: \n")

	cmdScanner := bufio.NewScanner(cmdReader)
	errScanner := bufio.NewScanner(errReader)

	timeStart := time.Now()
	go func() {
		color.Set(color.FgYellow)
		for cmdScanner.Scan() {
			fmt.Printf("[%s]fox up | %s \n", time.Now().Format("15:04:05"), cmdScanner.Text())
		}
		color.Unset()
	}()

	go func() {
		color.Set(color.FgRed)
		for errScanner.Scan() {
			fmt.Printf("[%s]fox up | %s \n", time.Now().Format("15:04:05"), errScanner.Text())
		}
		color.Unset()
	}()

	err = cmd.Start()
	if err != nil {
		log.Fatal(err)
	}

	err = cmd.Wait()
	if err != nil {
		log.Fatal(err)
	}
	timeEnd := time.Now()

	fmt.Printf("Fox Up Completed!")
	fmt.Printf("Total Duration: %v", timeEnd.Sub(timeStart))
}
