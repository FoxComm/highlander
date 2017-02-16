defmodule Hyperion.Amazon.Templates.Categories.ClothingAccessories do

  # TODO: Add variation data processing

  def template_string do
    """
    <ClothingAccessories>
      <%= if variation == true do %>
        <VariationData>
          <Parentage><%= parentage %></Parentage>
          <Color><%= color %></Color>
          <!--<Size><%= size %></Size>-->
          <VariationTheme>SizeColor</VariationTheme>
        </VariationData>
      <% end %>
      <ClassificationData>
        <Department><%= department %></Department>
      </ClassificationData>
    </ClothingAccessories>
    """
  end
end
