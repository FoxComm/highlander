defmodule Marketplace.Repo.Migrations.AddMerchantApplicationSocialProfiles do
  use Ecto.Migration

  def change do
    create table (:merchant_application_social_profiles) do
      add :merchant_application_id, references(:merchant_application)
      add :social_profile_id, references(:social_profile)
    end

  end
end
