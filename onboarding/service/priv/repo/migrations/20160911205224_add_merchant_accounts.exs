defmodule OnboardingService.Repo.Migrations.AddMerchantAccounts do
  use Ecto.Migration

  def change do
    create table(:merchant_accounts) do
      add :first_name, :string
      add :last_name, :string
      add :phone_number, :string
      add :business_name, :string
      add :description, :string
      add :email_address, :string
      add :password, :string
      add :solomon_id, :integer #id in solomon
      add :merchant_id, references(:merchants)

      timestamps
    end
  end
end
