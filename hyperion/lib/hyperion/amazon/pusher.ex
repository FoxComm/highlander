defmodule Hyperion.Amazon.Pusher do
  alias Hyperion.PhoenixScala.Client, warn: true
  alias Hyperion.Amazon.TemplateBuilder, warn: true
  alias Hyperion.Amazon, warn: true

  def push(product_id, cfg, jwt, purge \\ false) do
    product = Client.get_product(product_id, jwt)
    result = get_submisstion_result(product_id, purge)

    submit_product(product, cfg, false, result.product_feed)
    |> submit_variations(cfg, result.variations_feed)
    |> submit_price(cfg, result.price_feed)
    |> submit_inventory(cfg, result.inventory_feed)
    |> submit_images(cfg, result.images_feed)
  end

  defp submit_product(product, cfg, purge, nil) do
    tpl =
      Amazon.product_feed(product)
      |> TemplateBuilder.submit_product_feed(%{seller_id: cfg.seller_id, purge_and_replace: purge})

    case MWSClient.submit_product_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{product_feed: inspect(error)})
        raise "Submit_product error: " <> inspect(error)

      {:warn, warn} ->
        store_submition_result(product.body["id"], %{product_feed: warn["ErrorResponse"]})
        raise "Submit_product warning: " <> warn["ErrorResponse"]["Error"]["Message"]

      {_, resp} ->
        store_submition_result(product.body["id"], %{
          product_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]
        })

        product
    end
  end

  defp submit_product(product, _cfg, _purge, _result), do: product

  defp submit_variations(product, cfg, nil) do
    case product.body["variants"] do
      [] -> product
      _ -> submit_variations_data(product, cfg, nil)
    end
  end

  defp submit_variations(product, _cfg, _result), do: product

  defp submit_variations_data(product, cfg, nil) do
    tpl =
      Amazon.product_feed(product)
      |> TemplateBuilder.submit_variation_feed(%{seller_id: cfg.seller_id})

    case MWSClient.submit_variation_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{variations_feed: inspect(error)})
        raise "Submit_variations error: " <> inspect(error)

      {:warn, warn} ->
        store_submition_result(product.body["id"], %{variations_feed: warn["ErrorResponse"]})
        raise "Submit_variations warning: " <> warn["ErrorResponse"]["Error"]["Message"]

      {_, resp} ->
        store_submition_result(product.body["id"], %{
          variations_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]
        })

        product
    end
  end

  defp submit_price(product, cfg, nil) do
    tpl =
      Amazon.price_feed(product)
      |> TemplateBuilder.submit_price_feed(cfg)

    case MWSClient.submit_price_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{price_feed: inspect(error)})
        raise "Submit_price error: " <> inspect(error)

      {:warn, warn} ->
        store_submition_result(product.body["id"], %{price_feed: warn["ErrorResponse"]})
        raise "Submit_price warning: " <> warn["ErrorResponse"]["Error"]["Message"]

      {_, resp} ->
        store_submition_result(product.body["id"], %{
          price_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]
        })

        product
    end
  end

  defp submit_price(product, _cfg, _result), do: product

  defp submit_inventory(product, cfg, nil) do
    tpl =
      Amazon.inventory_feed(product)
      |> TemplateBuilder.submit_inventory_feed(%{seller_id: cfg.seller_id})

    case MWSClient.submit_inventory_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{inventory_feed: inspect(error)})
        raise "Submit_inventory error: " <> inspect(error)

      {:warn, warn} ->
        store_submition_result(product.body["id"], %{inventory_feed: warn["ErrorResponse"]})
        raise "Submit_inventory warning: " <> warn["ErrorResponse"]["Error"]["Message"]

      {_, resp} ->
        store_submition_result(product.body["id"], %{
          inventory_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]
        })

        product
    end
  end

  defp submit_inventory(product, _cfg, _), do: product

  defp submit_images(product, cfg, nil) do
    tpl =
      Amazon.images_feed(product)
      |> TemplateBuilder.submit_images_feed(cfg)

    case MWSClient.submit_images_feed(tpl, cfg) do
      {:error, error} ->
        store_submition_result(product.body["id"], %{images_feed: inspect(error)})
        raise "Submit_images error: " <> inspect(error)

      {:warn, warn} ->
        store_submition_result(product.body["id"], %{images_feed: warn["ErrorResponse"]})
        raise "Submit_images warning: " <> warn["ErrorResponse"]["Error"]["Message"]

      {_, resp} ->
        store_submition_result(product.body["id"], %{
          images_feed: resp["SubmitFeedResponse"]["SubmitFeedResult"]["FeedSubmissionInfo"]
        })

        get_submisstion_result(product.body["id"], false)
    end
  end

  defp submit_images(product, _cfg, _), do: get_submisstion_result(product.body["id"])

  defp store_submition_result(product_id, payload) do
    SubmissionResult.store_step_result(product_id, payload)
  end

  defp get_submisstion_result(product_id, force \\ false) do
    SubmissionResult.submission_result(product_id, force)
  end
end
