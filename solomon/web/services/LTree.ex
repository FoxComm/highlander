defmodule Solomon.LTree do

  @behaviour Postgrex.Extension

  # postgres already reads strings in for ltree 
  # and translates to string on output
  def init(_parameters, opts) when opts in [:reference, :copy], do: opts
  def matching(_opts), do: [type: "ltree"]
  def format(_opts), do: :text
  def encode(_type_info, bin, _types, _opts) when is_binary(bin), do: bin
  def decode(_type_info, bin, _types, :reference), do: bin
  def decode(_type_info, bin, _types, :copy), do: :binary.copy(bin)
end
