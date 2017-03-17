defmodule MWSAuthAgent do

  @moduledoc """
  Stores %MWSClient.Config{} in the memory via Agent
  """

  def start_link do
    Agent.start_link(fn -> MapSet.new end, name: __MODULE__)
  end

  def get(client_id) do
    case Agent.get(__MODULE__, &Map.get(&1, client_id)) do
      nil -> fetch_and_store(client_id)
      creds -> creds
    end
  end

  def store(cfg = %MWSClient.Config{}, client_id) do
    Agent.update(__MODULE__, &Map.put(&1, client_id, cfg))
  end

  def fetch_and_store(client_id) do
    Credentials.mws_config(client_id) |> store(client_id)
    get(client_id)
  end

  def refresh!(client_id) do
    Agent.get_and_update(__MODULE__, &Map.pop(&1, :client_id))
    fetch_and_store(client_id)
  end
end
