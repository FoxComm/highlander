import Ecto.Query

Hyperion.Repo.delete_all(Category)

path = Application.app_dir(:hyperion, "priv") <> "/seeds/categories/clothes_accessories_categories.csv"
data = File.stream!(path)
       |> CSV.decode

parse_and_store = fn row ->
  if String.length(Enum.at(row, 2)) > 0 do
    regex = ~r/department_name:(?<dep>[a-z-]+) AND item_type_keyword:(?<item>[a-z-]+)/
    captures = Regex.named_captures(regex, Enum.at(row, 2))
    Hyperion.Repo.insert!(%Category{node_id: String.to_integer(Enum.at(row, 0)),
                                             node_path: Enum.at(row, 1),
                                             department: captures["dep"],
                                             item_type: captures["item"] })
  end
end

Enum.map(data, fn row -> parse_and_store.(row) end)

from(c in Category, where: c.department == "NULL" and not is_nil(c.item_type)) |> Hyperion.Repo.update_all(set: [department: nil])

from(c in Category, where: like(c.node_path, "%Clothing%")) |> Hyperion.Repo.update_all(set: [category_name: "clothing"])
