package main

type SearchRow struct {
	ProductID int
	SkuID     int
	SkuCode   string
	Variants  map[string]string
}
