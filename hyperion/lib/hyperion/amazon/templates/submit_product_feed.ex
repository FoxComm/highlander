defmodule Hyperion.Amazon.Templates.SubmitProductFeed do
  use Timex

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
    <%= for {p, idx} <- products do %>
    <Message>
      <MessageID><%= idx %></MessageID>
      <OperationType>Update</OperationType>
        <Product>
          <SKU><%= p[:code] %></SKU>
          <%= Hyperion.Amazon.Templates.SubmitProductFeed.render_product_code(p) %>
          <%= Hyperion.Amazon.TemplateBuilder.render_field(p, :taxcode, "ProductTaxCode") %>
          <LaunchDate><%= Hyperion.Amazon.Templates.SubmitProductFeed.format_date_time(p[:activefrom]) %></LaunchDate>
          <DescriptionData>
            <Title><%= p[:title] %></Title>
            <Brand><%= p[:brand] %></Brand>
            <Description><%= HtmlSanitizeEx.strip_tags(p[:description]) %></Description>
            <%= Hyperion.Amazon.Templates.SubmitProductFeed.render_bullet_points(p) %>
            <%= Hyperion.Amazon.TemplateBuilder.render_field(p, :manufacturer, "Manufacturer") %>
            <%= for t <- p[:tags] do %>
              <SearchTerms><%= t %></SearchTerms>
            <% end %>
            <%= Hyperion.Amazon.TemplateBuilder.render_field(p, :item_type, "ItemType") %>
            <%= Hyperion.Amazon.TemplateBuilder.render_field(p, :isgiftwrapavailable, "IsGiftWrapAvailable") %>
            <%= Hyperion.Amazon.TemplateBuilder.render_field(p, :isgiftmessageavailable, "IsGiftMessageAvailable") %>
          </DescriptionData>
          <ProductData>
            <%= cond do %>
              <% p[:category] == "books" -> %>
                <%= Hyperion.Amazon.TemplateBuilder.books_category(p) %>
              <% p[:category] == "clothing" -> %>
                <%= Hyperion.Amazon.TemplateBuilder.clothing_category(p) %>
              <% true -> %>
                <% nil %>
            <% end %>
          </ProductData>
        </Product>
    </Message>
    <% end %>
    </AmazonEnvelope>
    """
  end

  def render_bullet_points(product) do
    points = Enum.filter(product, fn {k, _v} -> String.match?(to_string(k), ~r/bulletpoint/) end)

    for {k, v} <- points do
      unless v == nil && k == nil do
        """
        <BulletPoint>#{v}</BulletPoint>
        """
      end
    end
  end

  def render_product_code(list) do
    cond do
      Keyword.has_key?(list, :asin) ->
        """
        <StandardProductID>
        <Type>ASIN</Type>
        <Value>#{list[:asin]}</Value>
        </StandardProductID>
        """

      Keyword.has_key?(list, :upc) ->
        """
        <StandardProductID>
        <Type>UPC</Type>
        <Value>#{list[:upc]}</Value>
        </StandardProductID>
        """

      Keyword.has_key?(list, :ean) ->
        """
        <StandardProductID>
        <Type>EAN</Type>
        <Value>#{list[:ean]}</Value>
        </StandardProductID>
        """

      Keyword.has_key?(list, :isbn) ->
        """
        <StandardProductID>
        <Type>ISBN</Type>
        <Value>#{list[:isbn]}</Value>
        </StandardProductID>
        """

      true ->
        """
        #{nil}
        """
    end
  end

  def format_date_time(dt_str) do
    {:ok, dt} = Timex.Parse.DateTime.Parser.parse(dt_str, "{ISO:Extended:Z}")
    Timex.format(dt, "%Y-%m-%dT%H:%M:%S+00:00", :strftime) |> elem(1)
  end
end
