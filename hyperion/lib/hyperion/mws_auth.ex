defmodule Hyperion.MWSAuth do
  alias Hyperion.Amazon.Credentials

  @moduledoc """
  Stores %MWSClient.Config{} in the memory via Agent
  """

  def start_link do
    Agent.start_link(fn -> MapSet.new() end, name: __MODULE__)
  end

  def get(token) do
    case Agent.get(__MODULE__, &Map.get(&1, get_scope(token))) do
      nil -> fetch_and_store(token)
      creds -> creds
    end
  end

  def store(cfg = %MWSClient.Config{}, token) do
    Agent.update(__MODULE__, &Map.put(&1, get_scope(token), cfg))
  end

  def fetch_and_store(token) do
    Credentials.mws_config(token) |> store(token)
    get(token)
  end

  defp get_scope(token) do
    try do
      {:ok, data} = Hyperion.JwtAuth.verify(token)
      data[:scope]
    rescue
      _e in [RuntimeError, MatchError] ->
        raise NotAllowed
    end
  end
end
