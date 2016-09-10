defmodule Marketplace.MerchantController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.Merchant
  alias Marketplace.MerchantApplication

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
        |> render("merchant.json", merchant: merchant)
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
        |> render("merchant.json", merchant: merchant)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end


  def activate_application(conn, %{"application_id" => application_id}) do
    ma = Repo.get!(MerchantApplication, application_id)
    |> Repo.preload([:social_profile, :business_profile])
    merchant = %{
      name: ma.name,
      business_name: ma.name, 
      email_address: ma.email_address,
      description: ma.description,
      state: "activated"
    } 
    merchant_cs = Merchant.changeset(%Merchant{}, merchant)
    
    case Repo.insert(merchant_cs) do
      {:ok, inserted_merchant} -> 
        conn 
        |> put_status(:created)
        |> put_resp_header("location", merchant_path(conn, :show, inserted_merchant))
        |> render("merchant.json", merchant: inserted_merchant)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
