defmodule Marketplace.MerchantApplicationView do 
  use Marketplace.Web, :view

  def render("index.json", %{merchant_applications: merchant_applications}) do
    %{merchant_applications: render_many(merchant_applications, Marketplace.MerchantApplicationView, "merchant_application.json")}
  end

  def render("merchant_application.json", %{merchant_application: merchant_application}) do
    %{id: merchant_application.id,
      reference_number: merchant_application.reference_number,
      name: merchant_application.name,
      business_name: merchant_application.business_name,
      email_address: merchant_application.email_address,
      description: merchant_application.description,
      state: merchant_application.state}
  end

  def render("ma_with_merchant.json", %{merchant_application: merchant_application}) do
    %{merchant_application: 
      %{
        id: merchant_application.id,
        reference_number: merchant_application.reference_number,
        name: merchant_application.name,
        business_name: merchant_application.business_name,
        email_address: merchant_application.email_address,
        description: merchant_application.description,
        state: merchant_application.state, 
        merchant: %{
          id: merchant_application.merchant.id,
          name: merchant_application.merchant.name,
          description: merchant_application.merchant.description,
          state: merchant_application.merchant.state
        }
      }
    }
  end


  def render("show.json", %{merchant_application: merchant_application}) do 
    %{merchant_application: render_one(merchant_application, Marketplace.MerchantApplicationView, "merchant_application.json")}
  end
end
