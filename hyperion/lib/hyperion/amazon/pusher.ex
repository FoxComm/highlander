defmodule Hyperion.Amazon.Pusher do
  alias Hyperion.PhoenixScala.Client, warn: true
  alias Hyperion.Amazon, warn: true
  alias Hyperion.Amazon.TemplateBuilder, warn: true

  def push(product_id, cfg, jwt, purge, inventory) do
    product = Client.get_product(product_id, jwt)
    submit_product(product, cfg, purge)
    |> submit_price(cfg)
    |> submit_inventory(inventory, cfg)
    # |> submit_images(cfg)
  end

  def submit_product(product, cfg, purge) do
    tpl = Amazon.product_feed(product)
          |> TemplateBuilder.submit_product_feed(%{seller_id: cfg.seller_id, purge_and_replace: purge})

    case MWSClient.submit_product_feed(tpl, cfg) do
      {:error, error} ->
        IO.puts(inspect(error))
        store_submition_result(product.body["id"], %{product_feed: inspect(error)})
        raise "Submit_product error: " <> inspect(error)
      {:warn, warn} ->
        IO.puts(inspect(warn))
        store_submition_result(product.body["id"], %{product_feed: warn["ErrorResponse"]})
        raise "Submit_product warning: " <> warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} ->
        store_submition_result(product.body["id"],
                               %{product_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]})
        product
    end
  end

  def submit_price(product, cfg) do
    tpl = Amazon.price_feed(product)
          |> TemplateBuilder.submit_price_feed(cfg)

    case MWSClient.submit_price_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{price_feed: inspect(error)})
        raise "Submit_price error: " <> inspect(error)
      {:warn, warn} ->
        store_submition_result(product.body["id"], %{price_feed: warn["ErrorResponse"]})
        raise "Submit_price warning: " <> warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} ->
        store_submition_result(product.body["id"],
                               %{price_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]})
        product
        # SubmissionResult.submission_result(product.body["id"])
    end
  end

  def submit_inventory(product, inventory, cfg) do
    tpl = inventory
          |> Enum.with_index(1)
          |> TemplateBuilder.submit_inventory_feed(%{seller_id: cfg.seller_id})

    case MWSClient.submit_inventory_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{inventory_feed: inspect(error)})
        raise "Submit_inventory error: " <> inspect(error)
      {:warn, warn} ->
        store_submition_result(product.body["id"], %{inventory_feed: warn["ErrorResponse"]})
        raise "Submit_inventory warning: " <> warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} ->
        store_submition_result(product.body["id"],
                               %{inventory_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]})
        # product
        SubmissionResult.submission_result(product.body["id"])
    end
  end

  def submit_images(product, cfg) do
    list = Amazon.images_feed(product)
    tpl = TemplateBuilder.submit_images_feed(list, cfg)

    case MWSClient.submit_images_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product["id"], %{images_feed: inspect(error)})
        raise "Submit_images error: " <>  inspect(error)
      {:warn, warn} ->
        store_submition_result(product["id"], %{images_feed: warn["ErrorResponse"]})
        raise "Submit_images warning: " <> warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} ->
        store_submition_result(product["id"], %{images_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]})
        product
    end
  end

  def store_submition_result(product_id, payload) do
    SubmissionResult.store_step_result(product_id, payload)
  end

  def get_submisstion_result(product_id) do
    SubmissionResult.submission_result(product_id)
  end
end
