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

  def render("show.json", %{merchant_application: merchant_application}) do 
    %{merchant_application: render_one(merchant_application, Marketplace.MerchantApplicationView, "merchant_application.json")}
  end
end
