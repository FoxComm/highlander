defmodule Marketplace.Repo.Migrations.AddMerchantSocialProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_social_profiles) do
      add :merchant_id, references(:merchants)
      add :social_profile_id, references(:social_profiles)
    end
  end
end
