defmodule Marketplace.OriginIntegrationController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.OriginIntegration
  alias Marketplace.MerchantOriginIntegration
  alias Marketplace.OriginIntegrationView

  def create(conn, params), do: secured_route(conn, params, &create/3)
  defp create(conn, %{"origin_integration" => origin_integration_params, "user_id" => user_id}, claims) do
    case Repo.transaction(insert_and_relate(origin_integration_params, user_id)) do
      {:ok, %{origin_integration: origin_integration, merchant_origin_integration: m_oi}} ->
        conn
        |> put_status(:created)
        |> render(OriginIntegrationView, "origin_integration.json", origin_integration: origin_integration)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, params), do: secured_route(conn, params, &show/3)
  defp show(conn, %{"user_id" => s_id}, claims) do
    ma = Repo.get_by!(MerchantAccount, solomon_id: s_id)
    m_oi = Repo.get_by!(MerchantOriginIntegration, merchant_id: ma.merchant_id)
    |> Repo.preload(:origin_integration)

    conn
    |> render(OriginIntegrationView, "show.json", origin_integration: m_oi.origin_integration)
  end

  def update(conn, params), do: secured_route(conn, params, &update_/3)
  defp update_(conn, %{"user_id" => user_id, "origin_integration" => origin_integration_params}, claims) do
    ma = Repo.get_by!(MerchantAccount, solomon_id: user_id)
    m_oi = Repo.get_by!(MerchantOriginIntegration, merchant_id: ma.merchant_id)
    |> Repo.preload(:origin_integration)

    changeset = OriginIntegration.update_changeset(m_oi.origin_integration, origin_integration_params)
    case Repo.update(changeset) do
      {:ok, origin_integration} ->
        conn
        |> render(OriginIntegrationView, "origin_integration.json", origin_integration: origin_integration)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(origin_integration_params, user_id) do
    ma = Repo.get_by!(MerchantAccount, solomon_id: user_id)

    oi_cs = OriginIntegration.changeset(%OriginIntegration{}, origin_integration_params)
    Multi.new
    |> Multi.insert(:origin_integration, oi_cs)
    |> Multi.run(:merchant_origin_integration, fn %{origin_integration: origin_integration} ->
      map_origin_integration_to_merchant(origin_integration, ma.merchant_id) end
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
