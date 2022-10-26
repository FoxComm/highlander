defmodule Geronimo.Router.V1.Admin do
  use Maru.Router
  plug Geronimo.AuthPlug
  import Geronimo.Api.Utils

  alias Geronimo.{ContentType, Entity, Validation}

  require Logger

  # /v1/admin

  helpers do
    params :version do
      requires :ver, type: String, regexp: ~r/^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{0,6}Z$/
    end
  end

  namespace :content_types do
    desc "Get available content types"

    get do
      rows = ContentType.get_all(conn.assigns[:current_user].scope)
      respond_with(conn, rows)
    end

    desc "Get content type with given id"

    route_param :id do
      get do
        case ContentType.get(params[:id], conn.assigns[:current_user].scope) do
          {:ok, resp} -> respond_with(conn, resp)
          {:error, err} -> respond_with(conn, %{error: err}, 404)
        end
      end
    end

    desc "Creates new content type"

    params do
      requires :name, type: String
      requires :schema, type: Any
    end

    post do
      case ContentType.create(params, conn.assigns[:current_user]) do
        {:ok, resp} -> respond_with(conn, resp)
        {:error, err} -> respond_with(conn, err, 422)
      end
    end # Creates new content type

    desc "Updates content type with given id"

    params do
      requires :name, type: String
      requires :schema, type: Any
    end

    route_param :id do
      put do
        case ContentType.update(params[:id], params, conn.assigns[:current_user].scope) do
          {:ok, resp} -> respond_with(conn, resp)
          {:error, err} -> respond_with(conn, err, 422)
        end
      end
    end # Updates content type with given id

    desc "Get specific version"

    params do
      use :version
    end

    route_param :id do
      get :versions do
        case ContentType.get_specific_version(String.to_integer(params[:id]),
                                              params[:ver],
                                              conn.assigns[:current_user].scope) do
          {:ok, version} -> respond_with(conn, version)
          {:error, err} -> respond_with(conn, err, 400)
        end
      end
    end # Get specific version

    desc "Restore specific version"

    params do
      use :version
    end

    route_param :id do
      put :restore do
        case ContentType.restore_version(String.to_integer(params[:id]),
                                         params[:ver],
                                         conn.assigns[:current_user].scope) do
          {:ok, version} -> respond_with(conn, version)
          {:error, err} -> respond_with(conn, err, 400)
        end
      end
    end # Restore specific version
  end # content_types

  namespace :entities do
    desc "Creates new entity"

    params do
      requires :content_type_id, type: Integer
      requires :content, type: Any
      requires :storefront, type: String
    end

    post do
      try do
        {:ok, content_type} = ContentType.get(params[:content_type_id], conn.assigns[:current_user].scope)
        validated = Validation.validate!(params[:content], content_type.schema)

        case Entity.create(validated, content_type, params[:storefront], conn.assigns[:current_user]) do
          {:ok, record} -> respond_with(conn, record)
          {:error, err} -> respond_with(conn, err, 400)
        end
      rescue e in Ecto.Ecto.NoResultsError ->
        raise %NotFoundError{message: e.message}
      end
    end # Creates new entity

    desc "Updates entity with given id"

    params do
      requires :content_type_id, type: String
      requires :content, type: Any
    end

    route_param :id do
      put do
        Logger.info(inspect(params))

        {:ok, content_type} = ContentType.get(params[:content_type_id], conn.assigns[:current_user].scope)
        validated = Validation.validate!(params[:content], content_type.schema)

        case Entity.update(params[:id], validated, conn.assigns[:current_user]) do
          {:ok, resp} -> respond_with(conn, resp)
          {:error, err} -> respond_with(conn, err, 422)
        end
      end
    end # Updates entity with given id

    desc "Get specific version"

    params do
      use :version
    end

    route_param :id do
      get :versions do
        case Entity.get_specific_version(String.to_integer(params[:id]),
                                         params[:ver],
                                         conn.assigns[:current_user].scope) do
          {:ok, version} -> respond_with(conn, version)
          {:error, err} -> respond_with(conn, err, 400)
        end
      end
    end # Get specific version

    desc "Restore specific version"

    params do
      use :version
    end

    route_param :id do
      put :restore do
        case Entity.restore_version(String.to_integer(params[:id]),
                                    params[:ver],
                                    conn.assigns[:current_user].scope) do
          {:ok, version} -> respond_with(conn, version)
          {:error, err} -> respond_with(conn, err, 400)
        end
      end
    end # Restore specific version
  end # entities
end
