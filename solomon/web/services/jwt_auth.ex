defmodule Solomon.JWTAuth do
  alias JsonWebToken.Algorithm.RsaUtil

  def sign(claims) do
    JsonWebToken.sign(claims, sign_opts)
  end

  def verify(token) do
    JsonWebToken.verify(token, verify_opts)
  end

  defp sign_opts, do: opts(:sign)
  defp verify_opts, do: opts(:verify)

  defp opts(action) do
    alg = "RS256"
    private_key = Application.get_env(:solomon, Solomon.JWTAuth)[:private_key]
    public_key = Application.get_env(:solomon, Solomon.JWTAuth)[:public_key]
    key = case action do
      :sign -> RsaUtil.private_key(private_key, "")
      :verify -> RsaUtil.public_key(public_key, "")
    end
    %{alg: alg, key: key}
  end
end
