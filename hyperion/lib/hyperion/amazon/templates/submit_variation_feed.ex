defmodule Hyperion.Amazon.Templates.SubmitVariationFeed do
  def template_string do
    """
    <?xml version="1.0" encoding="utf-8" ?>
    <AmazonEnvelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="amzn-envelope.xsd">
    <Header>
      <DocumentVersion>1.01</DocumentVersion>
      <MerchantIdentifier><%= seller_id %></MerchantIdentifier>
    </Header>
    <MessageType>Relationship</MessageType>
      <Message>
        <MessageID>1</MessageID>
        <OperationType>Update</OperationType>
        <Relationship>
          <%= Hyperion.Amazon.Templates.SubmitVariationFeed.render_parent(hd(variations))%>
          <%= for {item, _idx} <- tl(variations) do %>
            <Relation>
              <SKU><%= item[:code] %></SKU>
              <Type>Variation</Type>
            </Relation>
          <% end %>
        </Relationship>
      </Message>
    </AmazonEnvelope>
    """
  end

  def render_parent({parent, _idx}) do
    """
    <ParentSKU>#{parent[:code]}</ParentSKU>
    """
  end
end
