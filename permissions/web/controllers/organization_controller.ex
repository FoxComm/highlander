defmodule Permissions.OrganizationController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Organization

  def index(conn, _params) do
    organizations = Repo.all(Organization)
    render(conn, "index.json", organizations: organizations)
  end

  def create(conn, %{"organization" => organization_params}) do
    changeset = Organization.changeset(%Organization{}, organization_params)

    case Repo.insert(changeset) do
      {:ok, organization} -> 
        conn
        |> put_status(:created)
        |> put_resp_header("location", organization_path(conn, :show, organization))
        |> render("organization.json", organization: organization)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    organization = Repo.get!(Organization, id)
    render(conn, "show.json", organization: organization)
  end
end
