defmodule Geronimo.JwtAuth do
  alias JsonWebToken.Algorithm.RsaUtil

  @doc """
  Verifies and decodes JWT header
  """
  def verify(payload) do
    opts = %{alg: "RS256", key: key()}
    try do
      case JsonWebToken.verify(payload, opts) do
        {:ok, data} -> data
        _ -> raise %NotAllowedError{}
      end
    rescue RuntimeError ->
      raise %NotAllowedError{}
    end
  end

  def get_scope(token) do
    case verify(token) do
      {:ok, data} -> data[:scope]
      _ -> raise RuntimeError
    end
  end

  defp key do
    RsaUtil.public_key(Application.fetch_env!(:geronimo, :public_key), "")
  end
end
