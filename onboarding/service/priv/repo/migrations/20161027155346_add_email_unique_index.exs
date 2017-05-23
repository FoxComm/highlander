defmodule OnboardingService.Repo.Migrations.AddEmailUniqueIndex do
  use Ecto.Migration

  def change do
    execute("create unique index merchant_email on merchants (lower(email_address))")
    execute("create unique index merchant_account_email on merchant_accounts (lower(email_address))")
    execute("create unique index merchant_application_email on merchant_applications (lower(email_address))")
  end
end
