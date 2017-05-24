defmodule OnboardingService.JWTAuth do
  alias JsonWebToken.Algorithm.RsaUtil

  def verify(token) do
    JsonWebToken.verify(token, opts)
  end

  defp opts do
    alg = "RS256"
    public_key = Application.get_env(:onboarding_service, OnboardingService.JWTAuth)[:public_key]
    key = RsaUtil.public_key(public_key, "")
    %{alg: alg, key: key}
  end
end
