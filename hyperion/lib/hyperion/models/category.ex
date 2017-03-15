defmodule Category do
  use Ecto.Schema
  import Ecto.Query

  @derive {Poison.Encoder, only: [:node_id, :node_path, :department, :item_type, :size_opts, :category_name]}

  schema "amazon_categories" do
    field :node_id, :integer
    field :node_path
    field :department
    field :item_type
    field :size_opts
    field :category_name
    field :object_schema_id, :integer

    timestamps()
  end

  def get_category_data(node_id) do
    q = from c in Category,
        where: c.node_id in ^[node_id]
    case Hyperion.Repo.all(q) do
      [] -> []
      x -> [department: hd(x).department, item_type: hd(x).item_type, category: hd(x).category_name]
    end
  end

  def get_category_data(nil), do: []

end
