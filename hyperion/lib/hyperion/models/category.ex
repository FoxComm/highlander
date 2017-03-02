defmodule Category do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query
  alias Hyperion.Repo

  @derive {Poison.Encoder, only: [:node_id, :node_path, :department, :item_type, :size_opts]}

  schema "amazon_categories" do
    field :node_id, :integer
    field :node_path
    field :department
    field :item_type
    field :size_opts

    timestamps
  end

end
