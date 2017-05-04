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
          <%= Hyperion.Amazon.Templates.SubmitImages.render_any_image(image) %>
        <% end %>
    </AmazonEnvelope>
    """
  end

  def render_any_image({n, _idx}) when n == nil, do: ""

  def render_any_image({{sku, type, src}, message_id}) do
    """
    <Message>
      <MessageID>#{message_id}</MessageID>
      <OperationType>Update</OperationType>
      <ProductImage>
        <SKU>#{sku}</SKU>
        <ImageType>#{type}</ImageType>
        <ImageLocation>#{src}</ImageLocation>
      </ProductImage>
    </Message>
    """
  end

  def render_any_image({{sku, type, src, index}, message_id}) do
      """
      <Message>
        <MessageID>#{message_id}</MessageID>
        <OperationType>Update</OperationType>
        <ProductImage>
          <SKU>#{sku}</SKU>
          <ImageType>#{type}#{index}</ImageType>
          <ImageLocation>#{src}</ImageLocation>
        </ProductImage>
      </Message>
      """
  end
end
