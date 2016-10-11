defmodule Marketplace.MerchantProductsUploadController do
  use Marketplace.Web, :controller

  alias Marketplace.MerchantProductsUpload

  def index(conn, _params) do
    merchant_products_uploads = Repo.all(MerchantProductsUpload)
    render(conn, "index.json", merchant_products_uploads: merchant_products_uploads)
  end

  def create(conn, %{"merchant_products_upload" => merchant_products_upload_params}) do
    changeset = MerchantProductsUpload.changeset(%MerchantProductsUpload{}, merchant_products_upload_params)

    case Repo.insert(changeset) do
      {:ok, merchant_products_upload} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_products_upload_path(conn, :show, merchant_products_upload))
        |> render("show.json", merchant_products_upload: merchant_products_upload)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    merchant_products_upload = Repo.get!(MerchantProductsUpload, id)
    render(conn, "show.json", merchant_products_upload: merchant_products_upload)
  end

  def update(conn, %{"id" => id, "merchant_products_upload" => merchant_products_upload_params}) do
    merchant_products_upload = Repo.get!(MerchantProductsUpload, id)
    changeset = MerchantProductsUpload.changeset(merchant_products_upload, merchant_products_upload_params)

    case Repo.update(changeset) do
      {:ok, merchant_products_upload} ->
        render(conn, "show.json", merchant_products_upload: merchant_products_upload)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def delete(conn, %{"id" => id}) do
    merchant_products_upload = Repo.get!(MerchantProductsUpload, id)

    # Here we use delete! (with a bang) because we expect
    # it to always work (and if it does not, it will raise).
    Repo.delete!(merchant_products_upload)

    send_resp(conn, :no_content, "")
  end
end
