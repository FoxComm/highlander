defmodule OnboardingService.Repo.Migrations.MerchantAddress do
  use Ecto.Migration

  def change do
    create table  (:merchant_addresses) do
      add :name, :string
      add :address1, :string
      add :address2, :string
      add :city, :string
      add :state, :string
      add :zip, :string
      add :is_headquarters, :boolean
      add :phone_number, :string
      add :merchant_id, references(:merchants)

      timestamps
    end
  end
end
