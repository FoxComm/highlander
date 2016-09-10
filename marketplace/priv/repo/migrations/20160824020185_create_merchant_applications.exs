defmodule Marketplace.Repo.Migrations.CreateMerchantApplication do
  use Ecto.Migration

  def change do
    create table(:merchant_applications) do
      add :reference_number, :string
      add :name, :string
      add :business_name, :string
      add :email_address, :string
      add :description, :string
      add :state, :string

      timestamps
    end

    create unique_index(:merchant_applications, [:reference_number])
  end
end
