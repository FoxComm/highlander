package responses

import "database/sql"

func NewStringFromSqlNullString(nullString sql.NullString) *string {
	if nullString.Valid {
		return &nullString.String
	}

	return nil
}
