defmodule Hyperion.API do
  require Logger
  use Maru.Router
  plug(CORSPlug)
  plug(Plug.Logger)

  plug(
    Plug.Parsers,
    pass: ["*/*"],
    json_decoder: Poison,
    parsers: [:urlencoded, :json, :multipart]
  )

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
  Verifies JWT header and extracts scope of a current user
  """
  def customer_id(conn) do
    token = jwt(conn)

    try do
      {:ok, data} = Hyperion.JwtAuth.verify(token)
      data[:scope]
    rescue
      RuntimeError ->
        raise NotAllowed
    end
  end

  mount(Hyperion.Router.V1)

  rescue_from :all, as: e do
    Logger.error("Exception occured: #{inspect(e)}")

    st =
      case e do
        Maru.Exceptions.NotFound -> 404
        Unauthorized -> 401
        %NotAllowed{} -> 401
        %AmazonError{} -> 400
        %AmazonCredentialsError{} -> 400
        Maru.Exceptions.MethodNotAllowed -> 405
        _ -> 500
      end

    msg = if Map.has_key?(e, :message), do: e.message, else: inspect(e)

    conn
    |> put_status(st)
    |> json(%{error: msg})
  end
end
