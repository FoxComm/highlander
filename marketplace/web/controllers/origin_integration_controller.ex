defmodule Marketplace.OriginIntegrationController do
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.MerchantAccount
  alias Marketplace.OriginIntegration
  alias Marketplace.MerchantOriginIntegration
  alias Marketplace.OriginIntegrationView

  def show(conn, %{"user_id" => s_id}) do
    ma = Repo.get_by!(MerchantAccount, solomon_id: s_id)
    
    m_oi = Repo.get_by!(MerchantOriginIntegration, merchant_id: ma.merchant_id)
    |> Repo.preload(:origin_integration)

    conn
    |> render(OriginIntegrationView, "show.json", origin_integration: m_oi.origin_integration)
  end
end
