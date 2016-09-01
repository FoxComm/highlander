defmodule Permissions.OrganizationController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Organization

  def index(conn, _params) do
    organizations = Repo.all(Organization)
    render(conn, "index.json", organizations: organizations)
  end
end
