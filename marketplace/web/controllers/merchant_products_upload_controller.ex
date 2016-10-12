defmodule Marketplace.MerchantProductsUploadController do
  use Marketplace.Web, :controller

  alias Ecto.Multi
  alias Marketplace.ProductsUpload
  alias Marketplace.ProductsUploadView
  alias Marketplace.MerchantProductsUpload

  def index(conn, %{"merchant_id" => merchant_id}) do
    products_uploads = Repo.all(from pf in ProductsUpload,
                              join: mpf in MerchantProductsUpload,
                              where: pf.id == mpf.products_upload_id
                              and mpf.merchant_id == ^merchant_id,
                              select: pf)
    render(conn, ProductsUploadView, "index.json", products_uploads: products_uploads)
  end

  def create(conn, %{"products_upload" => products_upload_params, "merchant_id" => merchant_id}) do
    case Repo.transaction(insert_and_relate(products_upload_params, merchant_id)) do
      {:ok, %{products_upload: products_upload, merchant_products_upload: m_pf}} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", merchant_products_upload_path(conn, :index, merchant_id))
        |> render(ProductsUploadView, "products_upload.json", products_upload: products_upload)
      {:error, failed_operation, failed_value, changes_completed} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: failed_value)
    end
  end

  def show(conn, %{"id" => id}) do
    products_upload = Repo.get_by!(ProductsUpload, id: id)

    conn
    |> render(ProductsUploadView, "show.json", products_upload: products_upload)
  end

  def update(conn, %{"id" => id, "products_upload" => products_upload_params}) do
    products_upload = Repo.get_by!(ProductsUpload, id: id)
    changeset = ProductsUpload.update_changeset(products_upload, products_upload_params)
    case Repo.update(changeset) do
      {:ok, products_upload} ->
        conn
        |> render(ProductsUploadView, "products_upload.json", products_upload: products_upload)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  defp insert_and_relate(products_upload_params, merchant_id) do
    pf_cs = ProductsUpload.changeset(%ProductsUpload{}, products_upload_params)
    Multi.new
    |> Multi.insert(:products_upload, pf_cs)
    |> Multi.run(:merchant_products_upload, fn %{products_upload: products_upload} ->
      map_products_upload_to_merchant(products_upload, merchant_id) end
    )
  end

  defp map_products_upload_to_merchant(products_upload, merchant_id) do
    mpf_cs = MerchantProductsUpload.changeset(%MerchantProductsUpload{}, %{
        "merchant_id" => merchant_id,
        "products_upload_id" => products_upload.id
      })

    Repo.insert(mpf_cs)
  end
end
