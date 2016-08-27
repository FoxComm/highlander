defmodule Marketplace.Repo.Migrations.CreateMerchant do
  use Ecto.Migration

  def change do
    create table(:merchants) do
      add :name, :string
      add :description, :string
      add :state, :string

      timestamps
    end

    create unique_index(:merchants, [:name])
  end
end
