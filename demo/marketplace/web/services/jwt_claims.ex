defmodule Marketplace.JWTClaims do
  import Plug.Conn
  alias Marketplace.JWTAuth

  def get_claims(conn) do
    cookie = conn
    |> fetch_cookies("jwt")
    |> Map.get(:req_cookies)
    |> Map.fetch("JWT")
    case cookie do
      {:ok, token} ->
        case Marketplace.JWTAuth.verify(token) do
          {:ok, claims} -> {:ok, claims}
          {:error, _} -> {:error, %{errors: "JWT.invalid.signature"}}
        end
      :error ->
        {:error, %{errors: "JWT.required"}}
    end
  end

  def secured_route(conn, params, fun) do
    case get_claims(conn) do
      {:ok, claims} ->
        fun.(conn, params, claims)
      {:error, errors} ->
        conn
        |> put_status(:unauthorized)
        |> Phoenix.Controller.render(Marketplace.ErrorView, "error.json", errors)
    end
  end
end
