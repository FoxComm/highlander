defmodule Geronimo.Router.V1.User do
  use Maru.Router
  import Geronimo.Api.Utils

  alias Geronimo.{Entity, Repo}

  namespace :health do
    desc "Check geronimo health"
    get do
      conn
      |> put_status(204)
      |> text(nil)
      |> halt
    end
  end

  namespace :entities do
    desc "Get all created entities"

    get do
      entities = Repo.all(Entity)
      respond_with(conn, entities)
    end

    desc "Get entity with specific ID"

    route_param :id do
      get do
        case Repo.get(Entity, params[:id]) do
          nil -> respond_with(conn, %{error: "Not found"}, 404)
          resp -> respond_with(conn, resp)
        end
      end
    end
  end
end
