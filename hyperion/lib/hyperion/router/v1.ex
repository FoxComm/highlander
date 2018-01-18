defmodule Hyperion.Router.V1 do
  use Maru.Router

  require Logger

  alias Hyperion.Repo, warn: true
  alias Hyperion.Amazon, warn: true
  alias Hyperion.API, warn: true
  alias Hyperion.Amazon.TemplateBuilder, warn: true
  alias Hyperion.Amazon.CategorySuggester, warn: true
  alias Hyperion.Amazon.Pusher, warn: true
  alias Hyperion.MWSAuth, warn: true

  import Ecto.Query

  version "v1" do
    namespace :public do
      namespace :health do
        desc("Check hyperion health")

        get do
          conn
          |> put_status(204)
          |> text(nil)
          |> halt
        end
      end

      namespace :credentials do
        desc("Checks credentials for existence")

        get :status do
          try do
            c = MWSAuth.get(API.jwt(conn))

            if c.seller_id == "" && c.mws_auth_token == "" do
              respond_with(conn, %{credentials: false})
            else
              respond_with(conn, %{credentials: true})
            end
          rescue
            AmazonCredentialsError ->
              respond_with(conn, %{credentials: false})
          end
        end

        desc("Refresh credentials in cache")

        put :refresh do
          MWSAuth.fetch_and_store(API.jwt(conn))
          respond_with(conn, %{success: true})
        end
      end

      # credentials

      namespace :products do
        desc("Push product to amazon")

        params do
          optional(:purge, type: Boolean)
        end

        route_param :product_id do
          post :push do
            purge = if params[:purge], do: params[:purge], else: false
            cfg = MWSAuth.get(API.jwt(conn))
            jwt = API.jwt(conn)
            r = Pusher.push(params[:product_id], cfg, jwt, purge)
            respond_with(conn, r)
          end
        end

        desc("Get push result for a product")

        route_param :product_id do
          get :result do
            case SubmissionResult.submission_result(params[:product_id], false) do
              nil ->
                respond_with(
                  conn,
                  %{error: "Result for product ID: #{params[:product_id]} not found"},
                  404
                )

              res ->
                respond_with(conn, res)
            end
          end
        end

        desc("Get products by ids and submit them to the Amazon MWS")

        params do
          requires(:ids, type: CharList)
          optional(:purge, type: Boolean)
        end

        post do
          cfg = MWSAuth.get(API.jwt(conn))
          purge = if params[:purge], do: true, else: false

          products =
            Amazon.product_feed(params[:ids], API.jwt(conn))
            |> TemplateBuilder.submit_product_feed(%{
              seller_id: cfg.seller_id,
              purge_and_replace: purge
            })

          case MWSClient.submit_product_feed(products, cfg) do
            {:error, error} ->
              respond_with(conn, %{message: error}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end

        desc("Add products by asin")

        params do
          requires(:ids, type: CharList)
          optional(:purge, type: Boolean)
        end

        post :by_asin do
          cfg = MWSAuth.get(API.jwt(conn))
          purge = if params[:purge], do: true, else: false

          products =
            Amazon.product_feed(params[:ids], API.jwt(conn))
            |> TemplateBuilder.submit_product_by_asin(%{
              seller_id: cfg.seller_id,
              purge_and_replace: purge
            })

          case MWSClient.submit_product_by_asin(products, cfg) do
            {:error, error} ->
              respond_with(conn, %{message: error}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end

        namespace :search do
          desc("Search products by code or query")

          params do
            requires(:q, type: String)
          end

          get do
            case MWSClient.list_matching_products(params[:q], MWSAuth.get(API.jwt(conn))) do
              {:error, error} ->
                respond_with(conn, %{message: inspect(error)}, 422)

              {:warn, warn} ->
                respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

              {_, resp} ->
                respond_with(conn, resp)
            end
          end
        end

        # search

        namespace :find_by_asin do
          route_param :asin do
            desc("Searches product by ASIN code")

            get do
              asins = String.split(params[:asin], ",")

              case MWSClient.get_product_by_asin(asins, MWSAuth.get(API.jwt(conn))) do
                {:error, error} ->
                  respond_with(conn, inspect(error), 422)

                {:warn, warn} ->
                  respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

                {_, resp} ->
                  respond_with(conn, resp)
              end
            end
          end
        end

        # find_by_asin

        namespace :by_code do
          desc("Search product by any code")

          params do
            requires(:code, type: String)
            requires(:values, type: String)
          end

          get do
            values = String.split(params[:values], ",")

            case MWSClient.get_matching_product_for_id(
                   params[:code],
                   values,
                   MWSAuth.get(API.jwt(conn))
                 ) do
              {:error, error} ->
                respond_with(conn, inspect(error), 422)

              {:warn, warn} ->
                respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

              {_, resp} ->
                respond_with(conn, resp)
            end
          end
        end

        # by_code

        namespace :categories do
          route_param :asin do
            desc("Returns categories for given asin")

            get do
              Hyperion.API.jwt(conn)

              case MWSClient.get_product_categories_for_asin(
                     params[:asin],
                     MWSAuth.get(API.jwt(conn))
                   ) do
                {:error, error} ->
                  respond_with(conn, inspect(error), 422)

                {:warn, warn} ->
                  respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

                {_, resp} ->
                  respond_with(conn, resp)
              end
            end
          end
        end

        # categories
      end

      # products

      namespace :categories do
        desc("Search for Amazon `department` and `item-type' by `node_path'")

        params do
          requires(:node_path, type: String)
          optional(:from, type: Integer)
          optional(:size, type: Integer)
        end

        get do
          res =
            from(
              c in Category,
              limit: ^params[:size],
              offset: ^params[:from],
              where: ilike(c.node_path, ^"%#{String.downcase(params[:node_path])}%")
            )
            |> Hyperion.Repo.all()

          respond_with(conn, res)
        end

        desc("Suggests category for product by title")

        params do
          optional(:q, type: String)
          optional(:title, type: String)
          optional(:limit, type: Integer)
        end

        get :suggest do
          try do
            Logger.info("params: #{inspect(params)}")
            cfg = MWSAuth.get(API.jwt(conn))

            prms =
              case params[:limit] do
                nil -> Map.merge(params, %{limit: 15})
                _ -> params
              end

            res = CategorySuggester.suggest_categories(prms, cfg)
            respond_with(conn, res)
          rescue
            e in RuntimeError ->
              respond_with(conn, %{error: e.message}, 422)
          end
        end

        desc("Get category by amazon node_id")

        route_param :node_id do
          get do
            case Category.get_category_with_schema(params[:node_id]) do
              nil ->
                respond_with(
                  conn,
                  %{error: "Categories for node_id #{params[:node_id]} not found"},
                  404
                )

              r ->
                respond_with(conn, r)
            end
          end

          # get
        end

        # route_param
      end

      # categories

      namespace :orders do
        desc("Get all orders")

        params do
          optional(:fulfillment_channel, type: String)
          optional(:payment_method, type: String)
          optional(:order_status, type: String)
          optional(:buyer_email, type: String)

          optional(
            :last_updated_after,
            type: String,
            regexp: ~r/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z$/
          )
        end

        get do
          list = Enum.map(params, fn {k, v} -> {k, String.split(v, ",")} end)

          case MWSClient.list_orders(list, MWSAuth.get(API.jwt(conn))) do
            {:error, error} ->
              respond_with(conn, inspect(error), 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end

        desc("Get order details")

        route_param :order_id do
          get do
            case MWSClient.get_order([params[:order_id]], MWSAuth.get(API.jwt(conn))) do
              {:error, error} ->
                respond_with(conn, inspect(error), 422)

              {:warn, warn} ->
                respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

              {_, resp} ->
                respond_with(conn, resp)
            end
          end

          # get order details

          desc("Get order items")

          get :items do
            case MWSClient.list_order_items(params[:order_id], MWSAuth.get(API.jwt(conn))) do
              {:error, error} ->
                respond_with(conn, inspect(error), 422)

              {:warn, warn} ->
                respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

              {_, resp} ->
                respond_with(conn, resp)
            end
          end

          # get order details

          desc("Get full order in FC notation")

          get :full do
            order = Hyperion.Amazon.get_full_order(params[:order_id], API.jwt(conn))
            respond_with(conn, order)
          end

          # get full order
        end

        # get order details and items
      end

      # orders

      namespace :prices do
        desc("Submit prices for already submitted products")

        params do
          requires(:ids, type: CharList)
        end

        post do
          cfg = MWSAuth.get(API.jwt(conn))

          prices =
            Amazon.price_feed(params[:ids], API.jwt(conn))
            |> TemplateBuilder.submit_price_feed(cfg)

          case MWSClient.submit_price_feed(prices, MWSAuth.get(API.jwt(conn))) do
            {:error, error} ->
              respond_with(conn, %{message: inspect(error)}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end
      end

      # prices

      namespace :inventory do
        desc("Submit inventory for already submitted products")

        params do
          group :inventory, type: CharList |> List do
            requires(:sku, type: String)
            requires(:quantity, type: Integer)
          end
        end

        post do
          cfg = MWSAuth.get(API.jwt(conn))
          inv_list = params[:inventory] |> Enum.with_index(1)

          inventories =
            TemplateBuilder.submit_inventory_feed(inv_list, %{seller_id: cfg.seller_id})

          case MWSClient.submit_inventory_feed(inventories, cfg) do
            {:error, error} ->
              respond_with(conn, %{message: inspect(error)}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end
      end

      # inventory

      namespace :images do
        desc("submit images feed")

        params do
          requires(:ids, type: CharList)
        end

        post do
          cfg = MWSAuth.get(API.jwt(conn))

          images =
            Amazon.images_feed(params[:ids], API.jwt(conn))
            |> TemplateBuilder.submit_images_feed(cfg)

          case MWSClient.submit_images_feed(images, MWSAuth.get(API.jwt(conn))) do
            {:error, error} ->
              respond_with(conn, %{message: inspect(error)}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end
      end

      namespace :submission_result do
        route_param :feed_id do
          desc("Check result of submitted feed")

          get do
            case MWSClient.get_feed_submission_result(
                   params[:feed_id],
                   MWSAuth.get(API.jwt(conn))
                 ) do
              {:error, error} ->
                respond_with(conn, %{message: inspect(error)}, 422)

              {:warn, warn} ->
                respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

              {_, resp} ->
                respond_with(conn, resp)
            end
          end
        end
      end

      # submission_result

      namespace :subscribe do
        desc("Subscribe to notifications queue")

        params do
          requires(:queue_url, type: String)
        end

        post do
          case MWSClient.subscribe_to_sqs(params[:queue_url], MWSAuth.get(API.jwt(conn))) do
            {:error, error} ->
              respond_with(conn, %{message: inspect(error)}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end

        desc("Unubscribe from notifications queue")

        params do
          requires(:queue_url, type: String)
        end

        delete do
          case MWSClient.unsubscribe_from_sqs(params[:queue_url], MWSAuth.get(API.jwt(conn))) do
            {:error, error} ->
              respond_with(conn, %{message: inspect(error)}, 422)

            {:warn, warn} ->
              respond_with(conn, %{error: warn["ErrorResponse"]["Error"]["Message"]}, 400)

            {_, resp} ->
              respond_with(conn, resp)
          end
        end
      end

      # subscribe

      namespace :object_schema do
        desc("Fetch object schema by name")

        route_param :schema_name do
          get do
            schema = Repo.get_by(ObjectSchema, schema_name: params[:schema_name])

            case schema do
              nil -> respond_with(conn, %{error: "Not found"}, 404)
              s -> respond_with(conn, s)
            end
          end
        end

        # route_param

        desc("Get object schema by amazon category id")

        namespace :category do
          route_param :category_id do
            get do
              case Repo.get_by(Category, node_id: params[:category_id]) do
                nil ->
                  respond_with(conn, %{error: "Category not found"}, 404)

                x ->
                  schema = Repo.get(ObjectSchema, x.object_schema_id)
                  respond_with(conn, schema)
              end
            end
          end
        end

        desc("Get all available schemas")

        get do
          schemas =
            Repo.all(ObjectSchema)
            |> Enum.map(fn x -> %{id: x.id, name: x.schema_name} end)

          respond_with(conn, schemas)
        end
      end

      # object_schema
    end

    # public
  end

  # v1

  defp respond_with(conn, body, status \\ 200) do
    conn
    |> put_status(status)
    |> json(wrap(body))
    |> halt
  end

  defp wrap(collection) do
    if is_list(collection) do
      %{items: collection, count: Enum.count(collection)}
    else
      collection
    end
  end
end

# defmodule
