defmodule Elastic.Client do
  import Tirexs.HTTP

  def search(query, index_name, doc_type) do
    url = "#{elastic_path(index_name, doc_type)}/_search"
    post(url, query)
    |> process_response
  end

  def index_document(entity_id, index_name, doc_type, payload) do
    url = "#{elastic_path(index_name, doc_type)}/#{entity_id}"
    put(url, payload)
  end

  def elastic_path(index_name, doc_type) do
    url = Application.fetch_env!(:tirexs, :elastic_uri)
    "#{url}/#{index_name}/#{doc_type}"
  end

  def create_index(index_name) do
    post("#{elastic_path(index_name, nil)}", [])
  end

  def put_mapping(index_name, mapping) do
    url = "#{elastic_path(index_name, nil)}"
    put(url, mapping)
  end

  def delete_index(index_name) do
    delete("#{elastic_path(index_name, nil)}", [])
  end

  def process_response(response) do
    case response do
      {:ok, 200, res} ->
        format_map(res.hits)
      {_, _, err} ->
        err
    end
  end

  def format_map(data) do
    total = data.total
    %{items: Enum.map(data.hits, fn item -> item._source end), total: total}
  end
end