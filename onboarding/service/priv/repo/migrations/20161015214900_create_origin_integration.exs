defmodule OnboardingService.Repo.Migrations.CreateOriginIntegration do
  use Ecto.Migration

  def change do
    create table(:origin_integrations) do
      add :shopify_key, :string
      add :shopify_password, :string
      add :shopify_domain, :string

      timestamps()
    end

  end
end
