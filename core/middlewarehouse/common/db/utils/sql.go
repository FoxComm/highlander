package utils

import "database/sql"

func MakeSqlNullString(str *string) sql.NullString {
	if str == nil {
		return sql.NullString{String: "", Valid: false}
	}

	return sql.NullString{String: *str, Valid: true}
}
