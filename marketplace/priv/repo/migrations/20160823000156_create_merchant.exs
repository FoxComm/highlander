defmodule Marketplace.Repo.Migrations.CreateMerchant do
  use Ecto.Migration

  def change do
    create table(:merchants) do
      add :name, :string
      add :business_name, :string
      add :email_address, :string
      add :description, :string
      add :state, :string

      timestamps
    end

    create unique_index(:merchants, [:name])
  end
end
