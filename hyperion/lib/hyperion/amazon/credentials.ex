defmodule Hyperion.Amazon.Credentials do
  alias Hyperion.PhoenixScala.Client, warn: true

  def mws_config(token) do
    Client.get_credentials(token)
    |> build_cfg
  end

  defp build_cfg(client) do
    %MWSClient.Config{aws_access_key_id: mws_access_key_id(),
                      aws_secret_access_key: mws_secret_access_key(),
                      seller_id: client.seller_id,
                      mws_auth_token: client.mws_auth_token }
  end

  defp mws_secret_access_key do
    Application.fetch_env!(:hyperion, :mws_secret_access_key)
  end

  defp mws_access_key_id do
    Application.fetch_env!(:hyperion, :mws_access_key_id)
  end
end
