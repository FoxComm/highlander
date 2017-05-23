defmodule OnboardingService.Repo.Migrations.CreateMerchantApplication do
  use Ecto.Migration

  def change do
    create table(:merchant_applications) do
      add :reference_number, :binary_id
      add :name, :string
      add :business_name, :string
      add :phone_number, :string
      add :email_address, :string
      add :description, :string
      add :site_url, :string
      add :state, :string
      add :merchant_id, references(:merchants)

      timestamps
    end

    create unique_index(:merchant_applications, [:reference_number])
  end
end
