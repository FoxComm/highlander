defmodule OnboardingService.MerchantAccountView do 
  use OnboardingService.Web, :view

  def render("index.json", %{merchant_accounts: merchant_accounts}) do
    %{merchant_accounts: render_many(merchant_accounts, OnboardingService.MerchantAccountView, "show.json")}
  end

  def render("merchant_account.json", %{merchant_account: merchant_account}) do
    %{
      merchant_account: 
        %{
          id: merchant_account.id,
          first_name: merchant_account.first_name,
          last_name: merchant_account.last_name,
          phone_number: merchant_account.phone_number,
          business_name: merchant_account.business_name,
          description: merchant_account.description,
          email_address: merchant_account.email_address,
          solomon_id: merchant_account.solomon_id, # the corresponding account in solomon
          stripe_account_id: merchant_account.stripe_account_id
         }
     }
  end

  def render("show.json", %{merchant_account: merchant_account}) do 
    render_one(merchant_account, OnboardingService.MerchantAccountView, "merchant_account.json")
  end
end
