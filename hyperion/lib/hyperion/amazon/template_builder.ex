defmodule Hyperion.Amazon.TemplateBuilder do
  require EEx

  def submit_product_feed(list, opts) do
    data = [seller_id: opts.seller_id, purge_and_replace: opts.purge_and_replace,
            products: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitProductFeed.template_string, data)
  end

  def submit_product_by_asin(list, opts) do
    data = [seller_id: opts.seller_id, purge_and_replace: opts.purge_and_replace,
            products: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitAsin.template_string, data)
  end

  def submit_price_feed(list, opts) do
    data = [seller_id: opts.seller_id, prices: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitPriceFeed.template_string, data)
  end

  def submit_inventory_feed(list, opts) do
    data = [seller_id: opts.seller_id, inventory: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitInventoryFeed.template_string, data)
  end

  def submit_images_feed(list, opts) do
    data = [seller_id: opts.seller_id, images: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitImages.template_string, data)
  end

  def books_category(list) do
    EEx.eval_string(Hyperion.Amazon.Templates.Categories.Books.template_string, list)
  end

  def clothing_category(list) do
    EEx.eval_string(Hyperion.Amazon.Templates.Categories.ClothingAccessories.template_string, list)
  end

  def common_category(_list) do
    ""
  end
end