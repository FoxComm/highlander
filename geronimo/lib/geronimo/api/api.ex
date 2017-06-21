defmodule Geronimo.Api do
  require Logger
  use Maru.Router

  plug CORSPlug
  plug Plug.Logger
  plug Plug.Parsers,
    pass: ["*/*"],
    json_decoder: Poison,
    parsers: [:urlencoded, :json, :multipart]

  plug Plug.Session,
    store: :cookie,
    key: "_geronimo_key",
    signing_salt: "Jk7pxAMf"

  version "v1" do
    namespace :public do
      mount Geronimo.Router.V1.Public
    end

    namespace :admin do
      mount Geronimo.Router.V1.Admin
    end
  end

  def jwt(conn) do
    case List.keyfind(conn.req_headers, "jwt", 0) do
      {"jwt", token} -> token
      nil -> raise "No JWT header provided"
    end
  end

  rescue_from :all, as: e do
    Logger.error "Exception occured: #{inspect(e)}"
    st = case e do
      Maru.Exceptions.NotFound -> 404
      Unauthorized -> 401
      Maru.Exceptions.MethodNotAllowed -> 405
      %NotAllowedError{} -> 401
      %ForbiddenError{} -> 403
      %NotFoundError{} -> 404
      %CaseClauseError{} -> 422
      _ -> 500

    end
    Logger.error(inspect(e))

    msg = cond do
      Map.has_key?(e, :message) -> e.message
      Map.has_key?(e, :term) -> e.term
      true -> inspect(e)
    end

    conn
    |> put_status(st)
    |> json(%{error: msg})
  end
end
