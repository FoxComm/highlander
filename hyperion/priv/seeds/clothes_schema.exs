import Ecto.Query

Hyperion.Repo.delete_all(ObjectSchema)

name = "amazon_clothing"

schema = Application.app_dir(:hyperion, "priv") <> "/seeds/object_schemas/amazon_clothes_product.json"
         |> File.read!
         |> Poison.decode!

{:ok, object_schema} = ObjectSchema.changeset(%ObjectSchema{}, %{schema_name: name, schema: (schema)}) |> Hyperion.Repo.insert


(from c in Category, where: ilike(c.node_path, "%Clothing%"))
|> Hyperion.Repo.update_all(set: [object_schema_id: object_schema.id])