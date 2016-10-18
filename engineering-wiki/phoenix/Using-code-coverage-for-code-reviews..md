I'm interested in setting up a crank for FoxComm. This will take some work but, in the meantime, here's a description of how to quickly view untested areas of a codebase. It could be used as a tool to increase visibility when in a code review.

1. Build a coverage profile for the current package.

```
go test -coverprofile=coverage.out 
```

2. View it as HTML.

```
go tool cover -html=coverage.out
```

Untested code will be highlighted in red. 

Background reading - http://blog.golang.org/cover.