defmodule OnboardingService.Repo.Migrations.RenameApplicationProfileIndices do
  use Ecto.Migration

  def change do
    drop unique_index(:merchant_application_social_profiles, [:merchant_application_id])
    drop unique_index(:merchant_application_business_profiles, [:merchant_application_id])
    create unique_index(:merchant_application_business_profiles, [:merchant_application_id], name: :merch_app_business_index)
    create unique_index(:merchant_application_social_profiles, [:merchant_application_id], name: :merch_app_social_index)
  end
end
