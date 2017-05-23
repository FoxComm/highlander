defmodule OnboardingService.OriginIntegrationControllerTest do
  use OnboardingService.ConnCase

  alias OnboardingService.OriginIntegration
  @valid_attrs %{shopify_domain: "some content", shopify_key: "some content", shopify_password: "some content"}
  @invalid_attrs %{}

  test "lists all entries on index", %{conn: conn} do
    conn = get conn, origin_integration_path(conn, :index)
    assert html_response(conn, 200) =~ "Listing origin integrations"
  end

  test "renders form for new resources", %{conn: conn} do
    conn = get conn, origin_integration_path(conn, :new)
    assert html_response(conn, 200) =~ "New origin integration"
  end

  test "creates resource and redirects when data is valid", %{conn: conn} do
    conn = post conn, origin_integration_path(conn, :create), origin_integration: @valid_attrs
    assert redirected_to(conn) == origin_integration_path(conn, :index)
    assert Repo.get_by(OriginIntegration, @valid_attrs)
  end

  test "does not create resource and renders errors when data is invalid", %{conn: conn} do
    conn = post conn, origin_integration_path(conn, :create), origin_integration: @invalid_attrs
    assert html_response(conn, 200) =~ "New origin integration"
  end

  test "shows chosen resource", %{conn: conn} do
    origin_integration = Repo.insert! %OriginIntegration{}
    conn = get conn, origin_integration_path(conn, :show, origin_integration)
    assert html_response(conn, 200) =~ "Show origin integration"
  end

  test "renders page not found when id is nonexistent", %{conn: conn} do
    assert_error_sent 404, fn ->
      get conn, origin_integration_path(conn, :show, -1)
    end
  end

  test "renders form for editing chosen resource", %{conn: conn} do
    origin_integration = Repo.insert! %OriginIntegration{}
    conn = get conn, origin_integration_path(conn, :edit, origin_integration)
    assert html_response(conn, 200) =~ "Edit origin integration"
  end

  test "updates chosen resource and redirects when data is valid", %{conn: conn} do
    origin_integration = Repo.insert! %OriginIntegration{}
    conn = put conn, origin_integration_path(conn, :update, origin_integration), origin_integration: @valid_attrs
    assert redirected_to(conn) == origin_integration_path(conn, :show, origin_integration)
    assert Repo.get_by(OriginIntegration, @valid_attrs)
  end

  test "does not update chosen resource and renders errors when data is invalid", %{conn: conn} do
    origin_integration = Repo.insert! %OriginIntegration{}
    conn = put conn, origin_integration_path(conn, :update, origin_integration), origin_integration: @invalid_attrs
    assert html_response(conn, 200) =~ "Edit origin integration"
  end

  test "deletes chosen resource", %{conn: conn} do
    origin_integration = Repo.insert! %OriginIntegration{}
    conn = delete conn, origin_integration_path(conn, :delete, origin_integration)
    assert redirected_to(conn) == origin_integration_path(conn, :index)
    refute Repo.get(OriginIntegration, origin_integration.id)
  end
end
