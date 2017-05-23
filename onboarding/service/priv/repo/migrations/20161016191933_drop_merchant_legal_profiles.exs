defmodule OnboardingService.Repo.Migrations.DropeMerchantLegalProfiles do
  use Ecto.Migration

  def change do
    drop table(:merchant_legal_profiles)
    drop table(:legal_profiles)
  end
end
