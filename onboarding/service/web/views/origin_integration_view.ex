defmodule OnboardingService.OriginIntegrationView do
  use OnboardingService.Web, :view

  def render("origin_integration.json", %{origin_integration: origin_integration}) do
    %{id: origin_integration.id,
      shopify_key: origin_integration.shopify_key,
      shopify_password: origin_integration.shopify_password,
      shopify_domain: origin_integration.shopify_domain
    }
  end

  def render("show.json", %{origin_integration: origin_integration}) do
    %{origin_integration: render_one(origin_integration, OnboardingService.OriginIntegrationView, "origin_integration.json")}
  end
end
