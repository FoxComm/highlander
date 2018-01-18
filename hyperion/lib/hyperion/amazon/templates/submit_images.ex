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
        <%= Hyperion.Amazon.Templates.SubmitImages.render_main_image(hd(images)) %>
        <%= Hyperion.Amazon.Templates.SubmitImages.render_pt_images(hd(images)) %>
        <%= Hyperion.Amazon.Templates.SubmitImages.render_swatches(tl(images)) %>
    </AmazonEnvelope>
    """
  end

  def render_main_image([{main, message_id} | _]) do
    """
    <Message>
      <MessageID>#{message_id}</MessageID>
      <OperationType>Update</OperationType>
      <ProductImage>
        <SKU>#{main[:sku]}</SKU>
        <ImageType>Main</ImageType>
        <ImageLocation>#{main[:location]}</ImageLocation>
      </ProductImage>
    </Message>
    """
  end

  def render_pt_images([_ | pt_images]) do
    for {item, idx} <- pt_images do
      """
      <Message>
        <MessageID>#{idx}</MessageID>
        <OperationType>Update</OperationType>
        <ProductImage>
          <SKU>#{item[:sku]}</SKU>
          <ImageType>#{item[:type]}#{item[:id]}</ImageType>
          <ImageLocation>#{item[:location]}</ImageLocation>
        </ProductImage>
      </Message>
      """
    end
  end

  def render_swatches([]), do: ""

  def render_swatches([swatches | _]) do
    for swatch <- swatches do
      """
      <Message>
        <MessageID>#{swatch[:idx]}</MessageID>
        <OperationType>Update</OperationType>
        <ProductImage>
          <SKU>#{swatch[:sku]}</SKU>
          <ImageType>#{swatch[:type]}</ImageType>
          <ImageLocation>#{swatch[:location]}</ImageLocation>
        </ProductImage>
      </Message>
      """
    end
  end
end
