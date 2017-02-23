package utils

import (
	"bytes"
	"regexp"
)

// IsInSlice - returns true if needle is found in haystack
func IsInSlice(needle string, haystack []string) bool {
	found := false

	for _, item := range haystack {
		if item == needle {
			found = true
			break
		}
	}

	return found
}

// DiffSlices - returns list of needles not found in haystack
func DiffSlices(needles []string, haystack []string) []string {
	diff := []string{}

	for _, needle := range needles {
		if !IsInSlice(needle, haystack) {
			diff = append(diff, needle)
		}
	}

	return diff
}

func SanitizePassword(input []byte) string {
	r, _ := regexp.Compile("pass(?:word|wd)?\":\"(.+?)\"")

	return ReplaceAllMatchingGroup(string(input), "***", r)
}

func ReplaceAllMatchingGroup(in, repl string, r *regexp.Regexp) string {
	matches := r.FindAllStringSubmatchIndex(in, -1)

	var buffer bytes.Buffer

	if len(matches) > 0 {
		firstMatchFrom := matches[0][2]
		buffer.WriteString(in[:firstMatchFrom])
		buffer.WriteString(repl)

		for i := 1; i < len(matches); i++ {
			from := matches[i-1][3]
			to := matches[i][2]

			buffer.WriteString(in[from:to])
			buffer.WriteString(repl)
		}

		lastMatchTo := matches[len(matches)-1][3]
		buffer.WriteString(in[lastMatchTo:])
	}

	return buffer.String()
}
