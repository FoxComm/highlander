defmodule Marketplace.MerchantView do 
  use Marketplace.Web, :view

  def render("index.json", %{merchants: merchants}) do
    %{merchants: render_many(merchants, Marketplace.MerchantView, "merchant.json")}
  end

  def render("merchant.json", %{merchant: merchant}) do
    %{id: merchant.id,
      name: merchant.name,
      description: merchant.description,
      state: merchant.state}
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
