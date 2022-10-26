defmodule Geronimo.Validation do
  @moduledoc """
  This module transforms field definition into Vex validators and validates data
  """

  def validate!(data, spec) do
    case Vex.errors(d = Utils.atomize(data), get_validations(spec)) do
      [] -> {:ok, d}
      e -> e
    end
  end

  def get_validations(spec) do
     Utils.map_to_keyword(spec) |> Enum.into(%{})
    |> Enum.map(fn({k, v}) ->
      {k, validates(Keyword.fetch!(v, :type) |> String.to_atom) ++
          required(Keyword.fetch(v, :required))
      }
    end)
  end

  def required({:ok, true}), do: [presence: true]
  def required(_),           do: []

  def validates(:integer), do: [format: [with: ~r/^[0-9]*\z/,
                                message: "only digits accepted"]]
  def validates(:text),    do: []
  def validates(:string),  do: []
  def validates(:array),   do: [by: [function: &is_list/1, allow_blank: true]]
  def validates(:float),   do: [format: [with: ~r/^\d+(?:\.\d+)?\z/,
                                message: "should be like 123.45"]]
  def validates(:date),    do: [by: [function: &valid_date?/1, allow_blank: false]]

  def valid_date?(d) do
    case Timex.parse(d, "%FT%T.%fZ", :strftime) do
      {:error, _} -> {:error, "should be like 1970-01-01T00:00:00.0Z"}
      {:ok, _}    -> :ok
    end
  end
end