defmodule Hyperion.Amazon.Templates.SubmitImages do
  @doc """
  Renders whole images feed based on albums
  More info here https://sellercentral.amazon.com/gp/help/200386840?ie=UTF8&*Version*=1&*entries*=0&
  """
  def template_string do
    """
    <?xml version="1.0" encoding="utf-8" ?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:noNamespaceSchemaLocation="amzn-envelope.xsd">
      <Header>
        <DocumentVersion>1.01</DocumentVersion>
        <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
      </Header>
      <MessageType>ProductImage</MessageType>
      <%= for image <- images do %>
        <%= Hyperion.Amazon.Templates.SubmitImages.render_main(image[:albums][:main], image[:code]) %>
        <%= Hyperion.Amazon.Templates.SubmitImages.render_swatches(image[:albums][:swatches],
                                                                   image[:code],
                                                                   image[:albums][:main]) %>
      <% end %>
    </AmazonEnvelope>
    """
  end

  @doc """
  Renders main and alternate images starting with `1' index'
  """
  def render_main(list, sku) do
    images = Enum.with_index(list, 1)
    for {data, idx} <- images do
      case {data, idx} do
        {data, 1} ->
          """
          <Message>
            <MessageID>#{idx}</MessageID>
            <OperationType>Update</OperationType>
            <ProductImage>
              <SKU>#{sku}</SKU>
              <ImageType>Main</ImageType>
              <ImageLocation>#{String.replace(data["src"], "https", "http")}</ImageLocation>
            </ProductImage>
          </Message>
          """
        {data, _} ->
          """
          <Message>
            <MessageID>#{idx}</MessageID>
            <OperationType>Update</OperationType>
          <ProductImage>
            <SKU>#{sku}</SKU>
            <ImageType>PT#{idx + 1}</ImageType>
            <ImageLocation>#{String.replace(data["src"], "https", "http")}</ImageLocation>
          </ProductImage>
          </Message>
          """
      end
    end
  end

  @doc """
  Renders swatches images indexed by `intial' count + 1
  """
  def render_swatches(list, sku, initial) do
    offset = Enum.count(initial) + 1
    images = Enum.with_index(list, offset)
    for {image, idx} <- images do
      """
      <Message>
        <MessageID>#{idx}</MessageID>
        <OperationType>Update</OperationType>
        <ProductImage>
          <SKU>#{sku}</SKU>
          <ImageType>Swatch</ImageType>
          <ImageLocation>#{String.replace(image["src"], "https", "http")}</ImageLocation>
        </ProductImage>
      </Message>
      """
    end
  end
end
