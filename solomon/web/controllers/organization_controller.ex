defmodule Solomon.OrganizationController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Organization
  alias Solomon.ScopeService

  def index(conn, _params) do
    organizations = ScopeService.scoped_index(conn, Organization)
    render(conn, "index.json", organizations: organizations)
  end

  def create(conn, %{"organization" => organization_params}) do
    changeset = Organization.changeset(%Organization{}, organization_params)

    case Repo.insert(changeset) do
      {:ok, organization} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", organization_path(conn, :show, organization))
        |> render("show.json", organization: organization)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    organization = ScopeService.scoped_show(conn, Organization, id)
    case organization do
      {:error, changeset} ->
        conn
        |> put_status(:unauthorized)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
      _ ->
        render(conn, "show.json", organization: organization)
    end
  end

  def update(conn, %{"id" => id, "organization" => organization_params}) do
    organization = Repo.get!(Organization, id)
    changeset = Organization.update_changeset(organization, organization_params)
    case Repo.update(changeset) do
      {:ok, organization} ->
        conn
        |> render("show.json", organization: organization)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
