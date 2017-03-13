defmodule Hyperion.Router.V1 do
  use Maru.Router

  require Logger

  alias Hyperion.Repo, warn: true
  alias Hyperion.Amazon, warn: true
  alias Hyperion.API, warn: true
  alias Hyperion.Amazon.TemplateBuilder, warn: true
  alias Hyperion.Amazon.CategorySuggester, warn: true

  import Ecto.Query

  version "v1" do
    namespace :public do

      namespace :health do
        desc "Check hyperion health"
        get do
          conn
          |> put_status(204)
          |> text(nil)
          |> halt
        end
      end

      namespace :credentials do
        route_param :client_id do
          desc "Get MWS credentials for exact client"
          get do
            creds = Repo.get_by(Credentials, client_id: params[:client_id])
            case creds do
              nil -> respond_with(conn, %{error: "Not found"}, 404)
              c -> respond_with(conn, c)
            end
          end # get credentials for client
        end

        desc "Store new credentials"
        params do
          requires :mws_auth_token, type: String
          requires :seller_id, type: String
          requires :client_id, type: Integer
        end

        post do
          changeset = Credentials.changeset(%Credentials{}, params)
          try do
            case Repo.insert(changeset) do
              {:ok, creds} -> respond_with(conn, creds)
              {:error, changeset} -> respond_with(conn, changeset.errors, 422)
            end
          rescue _e in Ecto.ConstraintError ->
            respond_with(conn, %{error: "Credentials for this client (client_id: #{params[:client_id]}) is already here"}, 422)
          end
        end # create new credentials

        desc "Update credentials"
        params do
          optional :mws_auth_token, type: String
          optional :seller_id, type: String
        end

        route_param :client_id do
          put do
            try do
              creds = Repo.get_by!(Credentials, client_id: params[:client_id])
              changeset = Credentials.changeset(creds, params)
              case Repo.update(changeset) do
                {:ok, creds} -> respond_with(conn, creds)
                {:error, changeset} -> respond_with(conn, changeset.errors, 422)
              end
            rescue _e in Ecto.NoResultsError ->
              respond_with(conn, %{error: "Not found"}, 404)
            end
          end # update credentials
        end

        desc "Remove credentials for specific client"
        route_param :client_id do
          delete do
            try do
              creds = Repo.get_by!(Credentials, client_id: params[:client_id])
              case Repo.delete(creds) do
                {:ok, _} -> conn
                            |> put_status(204)
                            |> text(nil)
                {:error, err} -> respond_with(conn, %{error: err}, 422)
              end
            rescue _e in Ecto.NoResultsError ->
              respond_with(conn, %{error: "Credentials for client #{params[:client_id]} not found"}, 404)
            end
          end
        end # destroy credentials
      end # credentials

      namespace :products do
        desc "Get products by ids and submit them to the Amazon MWS"

        params do
          requires :ids, type: CharList
          optional :purge, type: Boolean
        end

        post do
          cfg = Credentials.mws_config(API.customer_id(conn))
          purge = if (params[:purge]), do: true , else: false

          products = Amazon.product_feed(params[:ids], API.jwt(conn))
                     |> TemplateBuilder.submit_product_feed(%{seller_id: cfg.seller_id,
                                                              purge_and_replace: purge})

          case MWSClient.submit_product_feed(products, cfg) do
            {:error, error} -> respond_with(conn, %{message: error}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end

        desc "Add products by asin"

        params do
          requires :ids, type: CharList
          optional :purge, type: Boolean
        end

        post :by_asin do
          cfg = Credentials.mws_config(API.customer_id(conn))
          purge = if (params[:purge]), do: true , else: false
          products = Amazon.product_feed(params[:ids], API.jwt(conn))
                    |> TemplateBuilder.submit_product_by_asin(%{seller_id: cfg.seller_id,
                                                                purge_and_replace: purge})

          case MWSClient.submit_product_by_asin(products, cfg) do
            {:error, error} -> respond_with(conn, %{message: error}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end

        namespace :search do
          desc "Search products by code or query"

          params do
            requires :q, type: String
          end

          get do
            case MWSClient.list_matching_products(params[:q], Credentials.mws_config(API.customer_id(conn))) do
              {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
              {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
              {_, resp} -> respond_with(conn, resp)
            end
          end
        end # search

        namespace :find_by_asin do
          route_param :asin do
            desc "Searches product by ASIN code"
            get do
              asins = String.split(params[:asin], ",")
              case MWSClient.get_product_by_asin(asins, Credentials.mws_config(API.customer_id(conn))) do
                {:error, error} -> respond_with(conn, inspect(error), 422)
                {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
                {_, resp} -> respond_with(conn, resp)
              end
            end
          end
        end # find_by_asin

        namespace :categories do
          route_param :asin do
            desc "Returns categories for given asin"
            get do
              Hyperion.API.jwt(conn)
              case MWSClient.get_product_categories_for_asin(params[:asin], Credentials.mws_config(API.customer_id(conn))) do
                {:error, error} -> respond_with(conn, inspect(error), 422)
                {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
                {_, resp} -> respond_with(conn, resp)
              end
            end
          end
        end # categories
      end # products

      namespace :categories do
        desc "Search for Amazon `department` and `item-type' by `node_path'"

        params do
          requires :node_path, type: String
          optional :from, type: Integer
          optional :size, type: Integer
        end

        get do
          res = (from c in Category, limit: ^params[:size], offset: ^params[:from],
                 where: ilike(c.node_path, ^"%#{String.downcase(params[:node_path])}%"))
                |> Hyperion.Repo.all
          respond_with(conn, res)
        end

        desc "Suggests category for product by title"

        params do
          optional :q, type: String
          optional :title, type: String
          optional :limit, type: Integer
        end

        get :suggest do
          try do
            cfg = Credentials.mws_config(API.customer_id(conn))
            prms = case params[:limit] do
                     nil -> Map.merge(params, %{limit: 15})
                     _ -> params
                   end
            res = CategorySuggester.suggest_categories(prms, cfg)
            respond_with(conn, res)
          rescue e in RuntimeError ->
            respond_with(conn, %{error: e.message}, 422)
          end
        end
      end # categories

      namespace :orders do
        desc "Get all orders"
        params do
          optional :fulfillment_channel, type: String
          optional :payment_method, type: String
          optional :order_status, type: String
          optional :buyer_email, type: String
          optional :last_updated_after, type: String, regexp: ~r/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/
        end

        get do
          # If no date given — use beginning of current month
          last_upd = case params[:last_updated_after] do
                      nil -> Timex.beginning_of_month(DateTime.utc_now)
                             |> Timex.format("%Y-%m-%dT%H:%M:%SZ", :strftime)
                             |> elem(1)
                      _ -> params[:last_updated_after]
                     end

          # Remove :last_updated_after and convert Map to KeywordList
          list = Map.drop(params, [:last_updated_after])
                 |>Enum.map(fn {k, v} -> {k, String.split(v, ",")}  end)

          case MWSClient.list_orders(list, last_upd, Credentials.mws_config(API.customer_id(conn))) do
            {:error, error} -> respond_with(conn, inspect(error), 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end
      end # orders

      namespace :prices do
        desc "Submit prices for already submitted products"

        params do
          requires :ids, type: CharList
        end

        post do
          cfg = Credentials.mws_config(API.customer_id(conn))
          prices = Amazon.price_feed(params[:ids], API.jwt(conn))
                   |> TemplateBuilder.submit_price_feed(cfg)
          case MWSClient.submit_price_feed(prices, Credentials.mws_config(API.customer_id(conn))) do
            {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end
      end # prices

      namespace :inventory do
        desc "Submit inventory for already submitted products"

        params do
          group :inventory, type: CharList |> List do
            requires :sku, type: String
            requires :quantity, type: Integer
          end
        end

        post do
          cfg = Credentials.mws_config(API.customer_id(conn))
          inv_list = params[:inventory] |> Enum.with_index(1)
          inventories = TemplateBuilder.submit_inventory_feed(inv_list, %{seller_id: cfg.seller_id})

          case MWSClient.submit_inventory_feed(inventories, cfg) do
            {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end
      end # inventory

      namespace :images do
        desc "submit images feed"

        params do
          requires :ids, type: CharList
        end

        post do
          cfg = Credentials.mws_config(API.customer_id(conn))
          images = Amazon.images_feed(params[:ids], API.jwt(conn))
                   |> TemplateBuilder.submit_images_feed(cfg)
          case MWSClient.submit_images_feed(images, Credentials.mws_config(API.customer_id(conn))) do
            {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end
      end

      namespace :submission_result do
        route_param :feed_id do
        desc "Check result of submitted feed"
          get do
            case MWSClient.get_feed_submission_result(params[:feed_id], Credentials.mws_config(API.customer_id(conn))) do
              {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
              {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
              {_, resp} -> respond_with(conn, resp)
            end
          end
        end
      end # submission_result

      namespace :subscribe do
        desc "Subscribe to notifications queue"

        params do
          requires :queue_url, type: String
        end

        post do
          case MWSClient.subscribe_to_sqs(params[:queue_url], Credentials.mws_config(API.customer_id(conn))) do
            {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end

        desc "Unubscribe from notifications queue"

        params do
          requires :queue_url, type: String
        end

        delete do
          case MWSClient.unsubscribe_from_sqs(params[:queue_url], Credentials.mws_config(API.customer_id(conn))) do
            {:error, error} -> respond_with(conn, %{message: inspect(error)}, 422)
            {:warn, warn} -> respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)
            {_, resp} -> respond_with(conn, resp)
          end
        end
      end # subscribe

      namespace :object_schema do
        desc "Fetch object schema by name"
        route_param :schema_name do
          get do
            schema = Repo.get_by(ObjectSchema, schema_name: params[:schema_name])
            case schema do
              nil -> respond_with(conn, %{error: "Not found"}, 404)
              s -> respond_with(conn, s)
            end
          end
        end # route_param


        desc "Get object schema by amazon category id"
        namespace :category do
          route_param :category_id do
            get do
              case Repo.get_by(Category, node_id: params[:category_id]) do
                nil -> respond_with(conn, %{error: "Category not found"}, 404)
                x ->
                  schema = Repo.get(ObjectSchema, x.object_schema_id)
                  respond_with(conn, schema)
              end
            end
          end
        end

        desc "Get all available schemas"
        get do
          schemas = Repo.all(ObjectSchema)
                    |> Enum.map(fn(x) -> %{id: x.id, name: x.schema_name} end)
          respond_with(conn, schemas)
        end
      end # object_schema
    end # public
  end # v1

  defp respond_with(conn, body, status \\ 200) do
    conn
    |> put_status(status)
    |> json(wrap(body))
    |> halt
  end

  defp wrap(collection) do
    if is_list(collection) do
      %{items: collection,
        count: Enum.count(collection) }
    else
      collection
    end
  end
end # defmodule
