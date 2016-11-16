package utils

// IsInSlice - returns true if needle is found in haystack
func IsInSlice(needle uint, haystack []uint) bool {
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
func DiffSlices(needles []uint, haystack []uint) []uint {
	diff := []uint{}

	for _, needle := range needles {
		if !IsInSlice(needle, haystack) {
			diff = append(diff, needle)
		}
	}

	return diff
}
func ToStringPtr(str string) *string {
	return &str
}
