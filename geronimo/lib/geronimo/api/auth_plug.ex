defmodule Geronimo.AuthPlug do
  use Maru.Middleware

  def call(conn, _opts) do
    try do
      case List.keyfind(conn.req_headers, "jwt", 0) do
        {"jwt", token} ->
          data = Geronimo.JwtAuth.verify(token)

          conn
          |> assign(:current_user, struct(Geronimo.User, data))

        _ ->
          raise RuntimeError
      end
    rescue
      RuntimeError ->
        raise %NotAllowedError{}
    end
  end
end
