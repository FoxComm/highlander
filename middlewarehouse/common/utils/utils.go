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

	return ReplaceAllMatchingGroup(string(input), "***", r, 1)
}

func ReplaceAllMatchingGroup(input, repl string, r *regexp.Regexp, group int) string {
	matches := r.FindAllStringSubmatchIndex(input, -1)

	if len(matches) > 0 {
		for _, m := range matches {
			bt := []byte(input)
			index := group * 2
			from := m[index]
			to := m[index+1]

			toReplace := string(bt[from:to])

			if repl != toReplace {
				return ReplaceAllMatchingGroup(ReplaceByIndices(input, repl, from, to), repl, r, group)
			}

		}
	}

	return input
}

func ReplaceByIndices(input, repl string, from, to int) string {
	return strings.Join([]string{input[:from], repl, input[to:]}, "")
}
