defmodule Hyperion.Amazon.Templates.SubmitAsin do
  def template_string do
    """
    <?xml version="1.0" ?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="amzn- envelope.xsd">
    <Header>
      <DocumentVersion>1.01</DocumentVersion>
      <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
    </Header>
    <MessageType>Product</MessageType>
    <PurgeAndReplace><%= purge_and_replace %></PurgeAndReplace>
    <Message>
      <MessageID>1</MessageID>
      <OperationType>Update</OperationType>
        <%= for p <- products do %>
          <Product>
            <SKU><%= p[:code] %></SKU>
            <StandardProductID>
              <Type>ASIN</Type>
              <Value><%= p[:asin] %></Value>
            </StandardProductID>
          </Product>
        <% end %>
    </Message>
    </AmazonEnvelope>
    """
  end
end
