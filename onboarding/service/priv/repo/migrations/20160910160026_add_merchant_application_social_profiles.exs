defmodule OnboardingService.Repo.Migrations.AddMerchantApplicationSocialProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_application_social_profiles) do
      add :merchant_application_id, references(:merchant_applications)
      add :social_profile_id, references(:social_profiles)
    end

    create unique_index(:merchant_application_social_profiles, [:merchant_application_id])
  end
end
