import React, { Component } from 'react';

import Content from './components/presentational/content/content.js';
import Example from './components/presentational/example/example.js';
import Section from './components/presentational/section/section.js';

class Api extends Component {
  render() {
    return (
      <div>
        <Section title="Product">
          <Content>
            <p>
              This is an object that represents a unique product within a
              storefront. It differs from <code>Variants</code>, which represent
              the unique variations of a product. To understand the difference,
              consider a store that sells jackets. Each jacket style would be a
              <code>Product</code>, while the size and color combinations
              of each style would be individual <code>Variants</code>.
            </p>
          </Content>
          <Example>
            This is an example response.
          </Example>
        </Section>
      </div>
    )
  }
}

export default Api;
