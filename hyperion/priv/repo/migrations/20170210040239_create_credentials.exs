defmodule Hyperion.Repo.Migrations.CreateCredentials do
  use Ecto.Migration

  def change do
    create table(:amazon_credentials) do
      add :client_id, :integer
      add :seller_id, :string
      add :mws_auth_token, :string

      timestamps
    end
  end
end
