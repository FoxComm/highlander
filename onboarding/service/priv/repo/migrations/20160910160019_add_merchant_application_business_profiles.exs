defmodule OnboardingService.Repo.Migrations.AddMerchantApplicationBusinessProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_application_business_profiles) do
      add :merchant_application_id, references(:merchant_applications)
      add :business_profile_id, references(:business_profiles)
    end

    create unique_index(:merchant_application_business_profiles, [:merchant_application_id])
  end
end
