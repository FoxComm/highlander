defmodule OnboardingService.Repo.Migrations.AddSocialProfiles do
  use Ecto.Migration

  def change do
    create table (:social_profiles) do
      add :twitter_handle, :string
      add :instagram_handle, :string
      add :google_plus_handle, :string
      add :facebook_url, :string

      timestamps
    end
  end
end
