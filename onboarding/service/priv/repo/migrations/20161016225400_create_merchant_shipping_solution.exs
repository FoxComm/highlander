defmodule OnboardingService.Repo.Migrations.CreateMerchantShippingSolution do
  use Ecto.Migration

  def change do
    create table(:merchant_shipping_solutions) do
      add :merchant_id, references(:merchants)
      add :shipping_solution_id, references(:shipping_solutions)
    end
  end
end
