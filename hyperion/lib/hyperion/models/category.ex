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

  # Categories elasticsearch mappings:
  # %{node_path: category.node_path,
  #   department: category.department,
  #   item_type: category.item_type,
  #   inserted_at: category.inserted_at,
  #   updated_at: category.updated_at}

  def elastic_url do
    Application.fetch_env!(:hyperion, :elastic_uri)
  end

  def index_name do
    "amazon_categories"
  end

  def doc_type do
    ["category"]
  end

  def mapping do
    %{
      properties: %{
        node_path: %{type: "text"},
        department: %{type: "text"},
        item_type: %{type: "text"},
        inserted_at: %{type: "date"},
        updated_at: %{type: "date"}
      }
    }
  end
end
