defmodule Category do

  use Ecto.Schema
  
  @derive {Poison.Encoder, only: [:node_id, :node_path, :department, :item_type, :size_opts]}

  schema "amazon_categories" do
    field :node_id, :integer
    field :node_path
    field :department
    field :item_type
    field :size_opts
    field :object_schema_id, :integer

    timestamps()
  end
end
