defmodule OnboardingService.Repo.Migrations.CreateBottledwaterExtension do
  use Ecto.Migration

  def change do
    execute "do $$ begin if exists (select * from pg_available_extensions where name = 'bottledwater') then create extension if not exists bottledwater; end if; end $$"
  end
end
