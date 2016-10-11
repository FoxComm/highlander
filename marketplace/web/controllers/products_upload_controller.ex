defmodule Marketplace.ProductsUploadController do
  use Marketplace.Web, :controller

  alias Marketplace.ProductsUpload

  def index(conn, _params) do
    products_uploads = Repo.all(ProductsUpload)
    render(conn, "index.json", products_uploads: products_uploads)
  end

  def create(conn, %{"products_upload" => products_upload_params}) do
    changeset = ProductsUpload.changeset(%ProductsUpload{}, products_upload_params)

    case Repo.insert(changeset) do
      {:ok, products_upload} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", products_upload_path(conn, :show, products_upload))
        |> render("show.json", products_upload: products_upload)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    products_upload = Repo.get!(ProductsUpload, id)
    render(conn, "show.json", products_upload: products_upload)
  end

  def update(conn, %{"id" => id, "products_upload" => products_upload_params}) do
    products_upload = Repo.get!(ProductsUpload, id)
    changeset = ProductsUpload.changeset(products_upload, products_upload_params)

    case Repo.update(changeset) do
      {:ok, products_upload} ->
        render(conn, "show.json", products_upload: products_upload)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Marketplace.ChangesetView, "error.json", changeset: changeset)
    end
  end

  def delete(conn, %{"id" => id}) do
    products_upload = Repo.get!(ProductsUpload, id)

    # Here we use delete! (with a bang) because we expect
    # it to always work (and if it does not, it will raise).
    Repo.delete!(products_upload)

    send_resp(conn, :no_content, "")
  end
end
