defmodule Permissions.RoleController do
  use Permissions.Web, :controller
  alias Permissions.Repo
  alias Permissions.Role

  def index(conn, _params) do 
    roles = Repo.all(Role)
    render(conn, "index.json", roles: roles)
  end
end

