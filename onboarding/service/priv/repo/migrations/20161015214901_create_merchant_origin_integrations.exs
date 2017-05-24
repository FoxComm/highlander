defmodule OnboardingService.Repo.Migrations.CreateMerchantOriginIntegration do
  use Ecto.Migration

  def change do
    create table(:merchant_origin_integrations) do
      add :merchant_id, references(:merchants)
      add :origin_integration_id, references(:origin_integrations)

      timestamps()
    end

  end
end
