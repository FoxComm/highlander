defmodule Marketplace.VendorController do 
  use Marketplace.Web, :controller
  alias Marketplace.Repo
  alias Marketplace.Vendor

  def index(conn, _params) do
    vendors = Repo.all(Vendor)
    render(conn, "index.json", vendors: vendors)
  end

  def create(conn, %{"vendor" => vendor_params}) do
    changeset = Vendor.changeset(%Vendor{}, vendor_params)
    
    case Repo.insert(changeset) do 
      {:ok, vendor} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", vendor_path(conn, :show, vendor))
        |> render("vendor.json", vendor:vendor)
      {:error, changeset} -> 
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end
end
