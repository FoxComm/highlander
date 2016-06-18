package gormfox

type Association interface {
	Relation() string
}

type HasOne string

func (h HasOne) Relation() string {
	return "has_one"
}

type HasMany string

func (h HasMany) Relation() string {
	return "has_many"
}

type ManyToMany struct {
	Name       string
	Collection interface{}
}

func (m ManyToMany) Relation() string {
	return "many2many"
}
