defmodule OnboardingService.Repo.Migrations.AddMerchantLegalProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_legal_profiles) do
      add :merchant_id, references(:merchants)
      add :legal_profile_id, references(:legal_profiles)
    end
    
    create unique_index(:merchant_legal_profiles, [:merchant_id])
  end
end
