defmodule Solomon.Plug.JWTScope do
  import Plug.Conn
  alias Solomon.JWTAuth

  def init(default), do: default

  def call(conn, _default) do
    scope_or_error =  get_req_jwt(conn)
                      |> extract_jwt_scope
    case scope_or_error do
      {:ok, scope} ->
        put_resp_header(conn, "scope", scope)
      {:error, errs} ->
        put_resp_header(conn, "scope_err", Poison.encode!(errs))
    end
  end

  defp get_req_jwt(conn) do
    cookies = fetch_cookies(conn).cookies
    case Map.fetch(cookies, "JWT") do
      {:ok, token} -> {:ok, [token]}
      :error ->
        case Map.fetch(cookies, "jwt") do
          {:ok, token} -> {:ok, [token]}
          :error -> get_header_jwt(conn)
        end
    end
  end

  defp get_header_jwt(conn) do
    case get_req_header(conn, "jwt") do
      [] -> {:error, %{errors: ["request.jwt.notfound"]}}
      token ->  {:ok, token}
    end
  end

  defp extract_jwt_scope({:error, err}), do: {:error, err}
  defp extract_jwt_scope({:ok, token}) do
    verified = token
             |> Enum.at(0)
             |> JWTAuth.verify
    case verified do
      {:ok, claims} -> {:ok, Map.get(claims, :scope)}
      {:error, _} -> {:error, %{errors: ["request.jwt.invalid"]}}
    end
  end
end
