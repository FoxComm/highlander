defmodule Marketplace.MerchantView do 
  use Marketplace.Web, :view

  def render("index.json", %{merchants: merchants}) do
    %{merchants: render_many(merchants, Marketplace.MerchantView, "merchant.json")}
  end

  def render("merchant.json", %{merchant: merchant}) do
    %{id: merchant.id,
      name: merchant.name,
      business_name: merchant.business_name,
      phone_number: merchant.phone_number,
      email_address: merchant.email_address,
      description: merchant.description,
      site_url: merchant.site_url,
      state: merchant.state,
      scope_id: merchant.scope_id,
      organization_id: merchant.organization_id,
      stripe_account_id: merchant.stripe_account_id
    }
  end

  def render("show.json", %{merchant: merchant}) do 
    %{merchant: render_one(merchant, Marketplace.MerchantView, "merchant.json")}
  end

  def render("already_approved.json", %{errors: errors}) do 
    %{error: "This merchant application has already been approved."}
  end

  def render("invalid_state.json", %{errors: errors}) do 
    %{error: "This merchant application has an invalid state."}
  end

end
