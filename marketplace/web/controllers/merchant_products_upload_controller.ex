defmodule Marketplace.MerchantProductsUploadController do
  use Marketplace.Web, :controller

  alias Ecto.Multi
  alias Marketplace.ProductsUpload
  alias Marketplace.ProductsUploadView
  alias Marketplace.MerchantProductsUpload

  def index(conn, params), do: secured_route(conn, params, &index/3)
  defp index(conn, %{"merchant_id" => merchant_id}, claims) do
    products_uploads = Repo.all(from pf in ProductsUpload,
                              join: mpf in MerchantProductsUpload,
                              where: pf.id == mpf.products_upload_id
                              and mpf.merchant_id == ^merchant_id,
                              select: pf)
    render(conn, ProductsUploadView, "index.json", products_uploads: products_uploads)
  end

  def create(conn, params), do: secured_route(conn, params, &create/3)
  defp create(conn, %{"products_upload" => products_upload_params, "merchant_id" => merchant_id}, claims) do
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

  def show(conn, params), do: secured_route(conn, params, &show/3)
  defp show(conn, %{"id" => id}, claims) do
    products_upload = Repo.get_by!(ProductsUpload, id: id)

    conn
    |> render(ProductsUploadView, "show.json", products_upload: products_upload)
  end

  def update(conn, params), do: secured_route(conn, params, &update_/3)
  defp update_(conn, %{"id" => id, "products_upload" => products_upload_params}, claims) do
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
