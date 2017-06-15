defmodule Maru.Types.Any do
  @moduledoc """
  Allows maru to acceps JSON with any structure to create custom JSON document
  """
  use Maru.Type

  def parse(input, _), do: input
end
