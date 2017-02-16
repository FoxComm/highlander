defmodule MWSAuthAgent do

  @moduledoc """
  Stores %MWSClient.Config{} in the memory via Agent
  """

  def start_link do
    Agent.start_link(fn -> MapSet.new end, name: __MODULE__)
  end

  def get(client_id) do
    case Agent.get(__MODULE__, &Map.get(&1, :creds)) do
      nil -> fetch_and_store(client_id)
      creds -> creds
    end
  end

  def store(cfg = %MWSClient.Config{}) do
    Agent.update(__MODULE__, &Map.put(&1, :creds, cfg))
  end

  def fetch_and_store(client_id) do
    Credentials.mws_config(client_id) |> store
    get(client_id)
  end
end
