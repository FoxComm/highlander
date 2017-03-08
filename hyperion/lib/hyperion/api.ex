defmodule Hyperion.API do
  require Logger
  use Maru.Router
  plug CORSPlug
  plug Plug.Logger

  plug Plug.Parsers,
      pass: ["*/*"],
      json_decoder: Poison,
      parsers: [:urlencoded, :json, :multipart]

  @doc """
  Returns contents of JWT header
  """
  def jwt(conn) do
    case List.keyfind(conn.req_headers, "jwt", 0) do
      {"jwt", token} -> token
      nil -> raise "No JWT header provided"
    end
  end

  @doc """
  Returns contents of cutomer_id header
  """
  def customer_id(conn) do
    case List.keyfind(conn.req_headers, "customer_id", 0) do
      {"customer_id", id} -> id
      nil -> raise "No customer_id header provided"
    end
  end

  namespace :api do
    mount Hyperion.Router.V1
  end

  rescue_from :all, as: e do
    Logger.error "Exception occured: #{inspect(e)}"
    st = case e do
      Maru.Exceptions.NotFound -> 404
      Unauthorized -> 401
      Maru.Exceptions.MethodNotAllowed -> 405
      _ -> 500
    end

    conn
    |> put_status(st)
    |> json(%{error: inspect(e)})
  end
end
