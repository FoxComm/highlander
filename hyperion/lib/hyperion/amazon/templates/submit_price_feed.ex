defmodule Hyperion.Amazon.Templates.SubmitPriceFeed do
  def template_string do
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="amzn-envelope.xsd">
      <Header>
        <DocumentVersion>1.01</DocumentVersion>
        <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
      </Header>
      <MessageType>Price</MessageType>
      <Message>
        <MessageID>1</MessageID>
        <%= for p <- prices do %>
          <Price>
            <SKU><%= p[:code] %></SKU>
            <StandardPrice currency="<%= p[:retailprice]["currency"] %>"><%= p[:retailprice]["value"] / 100 %></StandardPrice>
          </Price>
        <% end %>
      </Message>
    </AmazonEnvelope>
    """
  end
end
