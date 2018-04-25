defmodule Category do
  use Ecto.Schema
  import Ecto.Query

  @derive {Poison.Encoder,
           only: [
             :node_id,
             :node_path,
             :department,
             :item_type,
             :size_opts,
             :category_name,
             :approve_needed
           ]}

  schema "amazon_categories" do
    field(:node_id, :integer)
    field(:node_path)
    field(:department)
    field(:item_type)
    field(:size_opts)
    field(:category_name)
    field(:object_schema_id, :integer)
    field(:approve_needed, :boolean)

    timestamps()
  end

  def get_category_data(node_id) do
    case node_id do
      nil -> []
      _ -> fetch_data(node_id)
    end
  end

  def fetch_data(node_id) do
    q = from(c in Category, where: c.node_id in ^[node_id])

    case Hyperion.Repo.all(q) do
      [] ->
        []

      x ->
        [department: hd(x).department, item_type: hd(x).item_type, category: hd(x).category_name]
    end
  end

  def get_category_with_schema(node_id) do
    q =
      from(
        c in Category,
        where: c.node_id == ^node_id and not is_nil(c.department) and not is_nil(c.item_type)
      )

    case Hyperion.Repo.one(q) do
      nil ->
        nil

      category ->
        %{category: category, schema: Hyperion.Repo.get(ObjectSchema, category.object_schema_id)}
    end
  end
end
