package command

import (
	"github.com/urfave/cli"
	"fmt"
	"os/exec"
	"log"
	"bufio"
	"time"
)


func CmdUp(c *cli.Context) {
	// Write some code here
	fmt.Fprintf(c.App.Writer, "Deploying and provisioning the Fox environment...")

	cmdName := "vagrant"
	cmdArgs := []string{"up"}

	cmd := exec.Command(cmdName, cmdArgs...)
	cmdReader, err := cmd.StdoutPipe()

	if err != nil {
		log.Fatal(err)
	}

	fmt.Printf("Running Vagrant Up.. Output below: \n")

	scanner := bufio.NewScanner(cmdReader)

	timeStart := time.Now()
	go func() {
		for scanner.Scan() {
			fmt.Printf("[%s]fox up | %s \n", time.Now().Format("15:04:05"), scanner.Text())
		}
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
