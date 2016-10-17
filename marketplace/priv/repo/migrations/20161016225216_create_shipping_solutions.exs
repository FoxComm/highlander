defmodule Marketplace.Repo.Migrations.CreateShippingSolutions do
  use Ecto.Migration

  def change do
    create table(:shipping_solutions) do
      add :carrier_name, :string
      add :price, :string

      timestamps()
    end
  end
end
