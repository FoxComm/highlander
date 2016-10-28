defmodule Marketplace.MerchantController do 
  use Marketplace.Web, :controller
  alias Ecto.Multi
  alias Marketplace.Repo
  alias Marketplace.Merchant
  alias Marketplace.MerchantApplication
  alias Marketplace.MerchantApplicationBusinessProfile
  alias Marketplace.MerchantApplicationSocialProfile
  alias Marketplace.MerchantSocialProfile
  alias Marketplace.MerchantBusinessProfile
  alias Marketplace.PermissionManager

  def index(conn, _params) do
    merchants = Repo.all(Merchant)
    render(conn, "index.json", merchants: merchants)
  end

  def create(conn, %{"merchant" => merchant_params}) do
    changeset = Merchant.changeset(%Merchant{}, merchant_params)
    
    case Repo.insert(changeset) do 
      {:ok, merchant} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_path(conn, :show, merchant))
        |> render("show.json", merchant: merchant)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    merchant = Repo.get!(Merchant, id)
    render(conn, "show.json", merchant: merchant)
  end

  def update(conn, %{"id" => id, "merchant" => merchant_params}) do 
    merchant = Repo.get!(Merchant, id)
    changeset = Merchant.update_changeset(merchant, merchant_params)
    case Repo.update(changeset) do
      {:ok, merchant} -> 
        conn 
        |> render("show.json", merchant: merchant)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end


  def activate_application(conn, %{"application_id" => application_id}) do
    ma = Repo.get!(MerchantApplication, application_id)
    |> Repo.preload([:social_profile, :business_profile])

    case ma.state do
      state when state == "approved" -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render("already_approved.json", %{errors: "error"})
      state when state == "new" ->
        #AWTODO: Later, we'll want to detect the parent scope, if any, and create underneath it.
        scope_id = PermissionManager.create_scope(conn)
        organization_id = PermissionManager.create_organization_from_merchant_application(conn, ma, scope_id)

        merchant = %{
          name: ma.name,
          business_name: ma.business_name,
          email_address: ma.email_address,
          site_url: ma.site_url,
          phone_number: ma.phone_number,
          description: ma.description,
          state: "activated",
          scope_id: scope_id,
          organization_id: organization_id
        } 
        merchant_cs = Merchant.changeset(%Merchant{}, merchant)

        multi_txn = Multi.new
        |> Multi.insert(:merchant, merchant_cs)
        |> Multi.run(:merchant_business_profile, fn %{merchant: merchant} -> 
          copy_business_profile_from_merchant_application(application_id, merchant) end)  
        |> Multi.run(:merchant_social_profile, fn %{merchant: merchant} -> 
          copy_social_profile_from_merchant_application(application_id, merchant) end)
        |> Multi.run(:merchant_application, fn %{merchant: merchant} ->
          update_merchant_application(application_id, merchant) end)

        case Repo.transaction(multi_txn) do
          {:ok, %{merchant: inserted_merchant, merchant_business_profile: m_bp, merchant_social_profile: m_sp}} -> 
            conn 
            |> put_status(:created)
            |> put_resp_header("location", merchant_path(conn, :show, inserted_merchant))
            |> render("show.json", merchant: inserted_merchant)
          {:error, target, changeset, errors} -> 
            conn
            |> put_status(:unprocessable_entity)
            |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
        end
      _ -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render("invalid_state.json", %{errors: "error"})
      end
  end 

  defp copy_business_profile_from_merchant_application(ma_id, merchant) do
    ma_bp = Repo.get_by(MerchantApplicationBusinessProfile, merchant_application_id: ma_id)

    case ma_bp do
      nil -> 
        {:ok, %MerchantBusinessProfile{}}
      ma_bp -> 
        m_bp = MerchantBusinessProfile.changeset(%MerchantBusinessProfile{}, %{
          "merchant_id" => merchant.id,
          "business_profile_id" => ma_bp.business_profile_id
        })
        Repo.insert(m_bp)
    end
  end

  defp copy_social_profile_from_merchant_application(ma_id, merchant) do
    ma_sp = Repo.get_by(MerchantApplicationSocialProfile, merchant_application_id: ma_id)

    case ma_sp do
      nil -> 
        {:ok, %MerchantSocialProfile{}}
      ma_sp ->
        m_sp = MerchantSocialProfile.changeset(%MerchantSocialProfile{}, %{
          "merchant_id" => merchant.id,
          "social_profile_id" => ma_sp.social_profile_id
        })
        Repo.insert(m_sp)
    end
  end

  defp update_merchant_application(ma_id, merchant) do
    ma = Repo.get(MerchantApplication, ma_id)
    ma_cs = MerchantApplication.changeset(ma, %{"state" => "approved", "merchant_id" => merchant.id})

    Repo.update(ma_cs)
  end
end
