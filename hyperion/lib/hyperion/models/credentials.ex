defmodule Credentials do
  use Ecto.Schema
  import Ecto.Changeset
  import Ecto.Query
  alias Hyperion.Repo

  @derive {Poison.Encoder, only: [:client_id, :mws_auth_token, :seller_id]}

  schema "amazon_credentials" do
    field :client_id, :integer
    field :mws_auth_token
    field :seller_id

    timestamps
  end

  def changeset(creds, params \\ %{}) do
    creds
    |> cast(params, [:client_id, :mws_auth_token, :seller_id])
    |> validate_required([:client_id, :mws_auth_token, :seller_id])
    |> unique_constraint(:seller_id)
  end


  def mws_config(client_id) do
    case Repo.get_by(Credentials, client_id: client_id) do
      nil -> %MWSClient.Config{}
      c -> build_cfg(c)
    end
  end

  defp build_cfg(client) do
    %MWSClient.Config{aws_access_key_id: mws_access_key_id(),
                      aws_secret_access_key: mws_secret_access_key(),
                      seller_id: client.seller_id,
                      mws_auth_token: client.mws_auth_token }
  end

  defp mws_secret_access_key do
    {:ok, secret} = Application.fetch_env(:hyperion, :mws_secret_access_key)
    secret
  end

  defp mws_access_key_id do
    {:ok, key} = Application.fetch_env(:hyperion, :mws_access_key_id)
    key
  end
end