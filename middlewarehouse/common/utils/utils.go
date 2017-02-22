package utils

import (
	"regexp"
	"strings"
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

	return ReplaceAll(string(input), "***", r)
}

func ReplaceAll(input, repl string, r *regexp.Regexp) string {
	matches := r.FindAllStringSubmatchIndex(input, -1)

	if len(matches) > 0 {
		for _, m := range matches {
			ru := []byte(input)
			toReplace := string(ru[m[2]:m[3]])

			if repl != toReplace {
				return ReplaceAll(ReplaceByIndices(input, repl, m[2], m[3]), repl, r)
			}

		}
	}

	return input
}

func ReplaceByIndices(input, repl string, from, to int) string {
	return strings.Join([]string{input[:from], repl, input[to:]}, "")
}
