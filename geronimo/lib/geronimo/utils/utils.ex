defmodule Utils do

  def atomize(map) do
    Enum.reduce(map, %{}, fn({k, v}, acc) ->
      Map.put(acc, String.to_atom(k), v)
    end)
  end

  def map_to_keyword(map), do: convert(map)

  def convert(map) when is_map(map), do: Enum.map(map, fn {k, v} -> {String.to_atom(k), convert(v)}  end)
  def convert(v) when is_list(v), do: hd(v)
  def convert(v), do: v
end
