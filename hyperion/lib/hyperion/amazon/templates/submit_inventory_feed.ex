defmodule Hyperion.Amazon.Templates.SubmitInventoryFeed do
  def template_string do
    """
    <?xml version="1.0" encoding="UTF-8"?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="amzn-envelope.xsd">
      <Header>
        <DocumentVersion>1.01</DocumentVersion>
        <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
      </Header>
      <MessageType>Inventory</MessageType>
      <Message>
        <MessageID>1</MessageID>
        <OperationType>Update</OperationType>
        <%= for i <- inventory do %>
          <Inventory>
            <SKU><%= i[:sku] %></SKU>
            <Quantity><%= i[:quantity] %></Quantity>
          </Inventory>
        <% end %>
      </Message>
    </AmazonEnvelope>
    """
  end
end