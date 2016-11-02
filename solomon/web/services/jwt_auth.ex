defmodule Solomon.JWTAuth do
  import JsonWebToken
  import JsonWebToken.Algorithm.RsaUtil

  def sign(claims) do
    sign(claims, sign_opts)
  end

  def verify(token) do
    verify(token, verify_opts)
  end

  defp sign_opts, do: opts(:sign)
  defp verify_opts, do: opts(:verify)

  defp opts(action) do
    alg = "RS256"
    private_key_path = Application.get_env(:solomon, Solomon.JWTAuth)[:private_key_path]
    public_key_path = Application.get_env(:solomon, Solomon.JWTAuth)[:public_key_path]
    private_key = Application.get_env(:solomon, Solomon.JWTAuth)[:private_key]
    public_key = Application.get_env(:solomon, Solomon.JWTAuth)[:public_key]
    key = case action do
      :sign -> private_key(private_key_path, private_key)
      :verify -> public_key(public_key_path, public_key)
    end
    %{alg: alg, key: key}
  end
end
