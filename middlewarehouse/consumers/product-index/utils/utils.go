package utils

func GetIntersection(arr1 []string, arr2 []string) []string {
	inter := []string{}
	for _, a1 := range arr1 {
		for _, a2 := range arr2 {
			if a1 == a2 {
				inter = append(inter, a1)
			}
		}
	}

	return inter
}
