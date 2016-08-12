package models

import "database/sql"

func NewSqlNullStringFromString(str *string) sql.NullString {
	if str == nil {
		return sql.NullString{String: "", Valid: false}
	}

	return sql.NullString{String: *str, Valid: true}
}
