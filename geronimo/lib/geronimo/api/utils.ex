defmodule Geronimo.Api.Utils do
  use Maru.Router

  def respond_with(conn, body, status \\ 200) do
    conn
    |> put_status(status)
    |> json(wrap(body))
    |> halt
  end

  defp wrap(collection) do
    if is_list(collection) do
      %{items: collection, count: Enum.count(collection)}
    else
      collection
    end
  end
end
