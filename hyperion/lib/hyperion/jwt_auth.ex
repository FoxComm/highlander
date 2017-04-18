defmodule Hyperion.JwtAuth do
  alias JsonWebToken.Algorithm.RsaUtil

  @doc """
  Verifies and decodes JWT header
  """
  def verify(payload) do
    opts = %{alg: "RS256", key: key()}
    JsonWebToken.verify(payload, opts)
  end

  @doc """
  Verifies token and returning scope
  """
  def get_scope(token) do
    case verify(token) do
      {:ok, data} -> data[:scope]
      _ -> raise RuntimeError
    end
  end

  defp key do
    RsaUtil.public_key(Application.fetch_env!(:hyperion, :public_key), "")
  end
end
