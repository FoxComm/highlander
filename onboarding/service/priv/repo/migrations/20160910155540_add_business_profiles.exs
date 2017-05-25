defmodule OnboardingService.Repo.Migrations.AddBusinessProfiles do
  use Ecto.Migration

  def change do
    create table (:business_profiles) do 
      add :monthly_sales_volume, :string
      add :target_audience, {:array, :string}
      add :categories, {:array, :string}

      timestamps
    end
  end
end
