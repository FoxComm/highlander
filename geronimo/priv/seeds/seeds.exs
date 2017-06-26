import Ecto.Query
Geronimo.Repo.delete_all(Geronimo.ContentType)
Geronimo.Repo.delete_all(Geronimo.Entity)

content_type_attrs = %{
  name: "BlogPost",
  schema: %{
    title: %{
    type: ["string"],
    required: true
    },
    body: %{
      type: ["string"],
      widget: "richText",
      required: true
    },
    author: %{
      type: ["string"],
      required: true
    },
    tags: %{
      type: ["array", []],
      required: false
    }},
  scope: "1",
  created_by: 1
}

entity_attrs = %{
  content: %{
    title: "Some title foooooo",
    body: "Lorem ipsum",
    author: "John Doe",
    tags: ["tag", "another"]
  }
}

Geronimo.ContentType.changeset(%Geronimo.ContentType{}, content_type_attrs)
|> Geronimo.Repo.insert

ct = (from c in Geronimo.ContentType, where: c.name == ^"BlogPost") |> Geronimo.Repo.one()

Geronimo.Entity.changeset(%Geronimo.Entity{}, Map.merge(entity_attrs, %{content_type_id: ct.id}))
|>Geronimo.Repo.insert