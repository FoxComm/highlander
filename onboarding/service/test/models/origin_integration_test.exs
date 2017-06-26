defmodule OnboardingService.OriginIntegrationTest do
  use OnboardingService.ModelCase

  alias OnboardingService.OriginIntegration

  @valid_attrs %{shopify_domain: "some content", shopify_key: "some content", shopify_password: "some content"}
  @invalid_attrs %{}

  test "changeset with valid attributes" do
    changeset = OriginIntegration.changeset(%OriginIntegration{}, @valid_attrs)
    assert changeset.valid?
  end

  test "changeset with invalid attributes" do
    changeset = OriginIntegration.changeset(%OriginIntegration{}, @invalid_attrs)
    refute changeset.valid?
  end
end
