defmodule Marketplace.MerchantApplicationController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.MerchantApplication

  def index(conn, _params) do
    merchant_applications = Repo.all(MerchantApplication)
    render(conn, "index.json", merchant_applications: merchant_applications)
  end

  def create(conn, %{"merchant_application" => merchant_application_params}) do
    changeset = MerchantApplication.changeset(%MerchantApplication{}, merchant_application_params)
    
    case Repo.insert(changeset) do 
      {:ok, merchant_application} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_application_path(conn, :show, merchant_application))
        |> render("merchant_application.json", merchant_application: merchant_application)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    merchant_application = Repo.get!(MerchantApplication, id)
    render(conn, "show.json", merchant_application: merchant_application)
  end

  def update(conn, %{"id" => id, "merchant_application" => merchant_application_params}) do 
    merchant_application = Repo.get!(MerchantApplication, id)
    changeset = MerchantApplication.update_changeset(merchant_application, merchant_application_params)
    case Repo.update(changeset) do
      {:ok, merchant_application} -> 
        conn 
        |> render("merchant_application.json", merchant_application: merchant_application)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
