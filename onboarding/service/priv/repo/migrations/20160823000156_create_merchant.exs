defmodule OnboardingService.Repo.Migrations.CreateMerchant do
  use Ecto.Migration

  def change do
    create table(:merchants) do
      add :name, :string
      add :business_name, :string
      add :phone_number, :string
      add :email_address, :string
      add :description, :string
      add :site_url, :string
      add :state, :string
      add :scope_id, :integer
      add :organization_id, :integer

      timestamps
    end

  end
end
