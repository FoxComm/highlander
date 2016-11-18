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
    scope_id = Map.get(organization_params, "scope_id")
    changeset = Organization.changeset(%Organization{}, organization_params)
                |> ScopeService.validate_scoped_changeset(conn, scope_id)

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
    scope_id = Map.get(organization_params, "scope_id", organization.scope_id)
    changeset = Organization.update_changeset(organization, organization_params)
                |> ScopeService.validate_scoped_changeset(conn, organization.scope_id)
                |> ScopeService.validate_scoped_changeset(conn, scope_id)
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
