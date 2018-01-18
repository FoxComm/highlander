defmodule Hyperion.Amazon.TemplateBuilder do
  require EEx

  def render_field(list, field_name, tag_name) do
    case Keyword.has_key?(list, field_name) do
      false -> nil
      _ -> "<#{tag_name}>#{Keyword.get(list, field_name)}</#{tag_name}>"
    end
  end

  def render_field(field, tag_name) do
    case field do
      nil -> nil
      _ -> "<#{tag_name}>#{field}</#{tag_name}>"
    end
  end

  def submit_product_feed(list, opts) do
    data = [seller_id: opts.seller_id, purge_and_replace: opts.purge_and_replace, products: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitProductFeed.template_string(), data)
  end

  def submit_variation_feed(list, opts) do
    data = [seller_id: opts.seller_id, variations: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitVariationFeed.template_string(), data)
  end

  def submit_product_by_asin(list, opts) do
    data = [seller_id: opts.seller_id, purge_and_replace: opts.purge_and_replace, products: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitAsin.template_string(), data)
  end

  def submit_price_feed(list, opts) do
    data = [seller_id: opts.seller_id, prices: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitPriceFeed.template_string(), data)
  end

  def submit_inventory_feed(list, opts) do
    data = [seller_id: opts.seller_id, inventory: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitInventoryFeed.template_string(), data)
  end

  def submit_images_feed(list, opts) do
    data = [seller_id: opts.seller_id, images: list]
    EEx.eval_string(Hyperion.Amazon.Templates.SubmitImages.template_string(), data)
  end

  def books_category(list) do
    EEx.eval_string(Hyperion.Amazon.Templates.Categories.Books.template_string(), assigns: list)
  end

  def clothing_category(list) do
    EEx.eval_string(
      Hyperion.Amazon.Templates.Categories.ClothingAccessories.template_string(),
      assigns: list
    )
  end

  def common_category(_list) do
    ""
  end
end
