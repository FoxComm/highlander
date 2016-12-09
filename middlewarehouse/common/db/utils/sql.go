package utils

import "database/sql"

func MakeSqlNullString(str *string) sql.NullString {
	if str == nil {
		return sql.NullString{String: "", Valid: false}
	}

	return sql.NullString{String: *str, Valid: true}
}

func CompareNullStrings(str1 sql.NullString, str2 sql.NullString) bool {
	return str1.Valid == str2.Valid && str1.String == str2.String
}
