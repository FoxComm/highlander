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

  def mapping do
    %{mappings: %{
      category: %{
        properties: %{
          node_path: %{type: "string", index: "not_analyzed" },
          department: %{type: "string"},
          item_type: %{type: "string", index: "not_analyzed"},
          node_id: %{type: "integer"}
        }
      }
    }
  }
  end

  def search(query_string, from \\ 0, size \\ 10) do
    # query = %{from: from, size: size, query: %{term: %{node_path: query_string}}}
    query = %{from: from, size: size,
              query: %{simple_query_string: %{query: "*#{String.downcase(query_string)}*",
                                              analyze_wildcard: true, default_operator: "AND"}}}
    Elastic.Client.search(query, index_name, doc_type)
  end

  def index(entity) do
    payload = [node_path: entity.node_path, department: entity.department,
               item_type: entity.item_type, node_id: entity.node_id]
    Elastic.Client.index_document(entity.id, index_name(), doc_type(), payload)
  end

  def elastic_url do
    Application.fetch_env!(:tirexs, :elastic_uri)
  end

  def index_name do
    "amazon_categories"
  end

  def doc_type do
    "category"
  end
end
