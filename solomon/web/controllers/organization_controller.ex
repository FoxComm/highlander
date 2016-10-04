defmodule Solomon.OrganizationController do
  use Solomon.Web, :controller
  alias Solomon.Repo
  alias Solomon.Organization

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
        |> render("show.json", organization: organization)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Solomon.ChangesetView, "errors.json", changeset: changeset)
    end
  end

  def show(conn, %{"id" => id}) do
    organization = Repo.get!(Organization, id)
    render(conn, "show.json", organization: organization)
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

  def create_admin_role(conn, %{"organization" => org_params}) do
    scope_id = Map.fetch!(org_params, "scope_id")
    role_cs = Role.changeset(%Role{}, %{name: "admin", scope_id: scope_id})
    case OrganizationService.create_role_with_permissions(role_cs, @admin_resources) do
      {:ok, role} ->
        conn
        |> put_status(:created)
        |> put_resp_header("location", role_path(conn, :show, role))
        |> render("show.json", role: role)
      {:error, changeset} ->
        conn
        |> put_status(:unprocessable_entity)
        |> render(Permissions.ChangesetView, "errors.json", changeset: changeset)
    end
  end
end
