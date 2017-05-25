defmodule OnboardingService.MerchantOriginIntegrationController do
  use OnboardingService.Web, :controller
  alias Ecto.Multi
  alias OnboardingService.Repo
  alias OnboardingService.OriginIntegration
  alias OnboardingService.MerchantOriginIntegration
  alias OnboardingService.OriginIntegrationView

  def create(conn, %{"origin_integration" => origin_integration_params, "merchant_id" => merchant_id}) do
    case Repo.transaction(insert_and_relate(origin_integration_params, merchant_id)) do
      {:ok, %{origin_integration: origin_integration, merchant_origin_integration: m_oi}} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_origin_integration_path(conn, :show, merchant_id))
        |> render(OriginIntegrationView, "origin_integration.json", origin_integration: origin_integration)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(OnboardingService.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"merchant_id" => m_id}) do
    m_oi = Repo.get_by!(MerchantOriginIntegration, merchant_id: m_id)
    |> Repo.preload(:origin_integration)

    conn
    |> render(OriginIntegrationView, "show.json", origin_integration: m_oi.origin_integration)
  end

  defp insert_and_relate(origin_integration_params, merchant_id) do
    oi_cs = OriginIntegration.changeset(%OriginIntegration{}, origin_integration_params)
    Multi.new
    |> Multi.insert(:origin_integration, oi_cs)
    |> Multi.run(:merchant_origin_integration, fn %{origin_integration: origin_integration} ->
      map_origin_integration_to_merchant(origin_integration, merchant_id) end
    )
  end

  defp map_origin_integration_to_merchant(origin_integration, merchant_id) do
    maoi_cs = MerchantOriginIntegration.changeset(%MerchantOriginIntegration{}, %{
        "merchant_id" => merchant_id,
        "origin_integration_id" => origin_integration.id
      })

    Repo.insert(maoi_cs)
  end
end
