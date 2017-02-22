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

func ReplaceAllMatchingGroup(in, repl string, r *regexp.Regexp, group int) string {
	matches := r.FindAllStringSubmatchIndex(in, -1)

	if len(matches) > 0 {
		shift := 0
		for _, match := range matches {
			from := match[2] + shift
			to := match[3] + shift

			in = strings.Join([]string{in[:from], repl, in[to:]}, "")
			// recalculate shift of match indices after replacing next occurrence
			shift = shift + len(repl) - (to - from)
		}

	}

	return in
}
