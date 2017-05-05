import React, { Component } from 'react';

import Content from './components/presentational/content/content.js';
import Example from './components/presentational/example/example.js';
import Section from './components/presentational/section/section.js';

class Api extends Component {
  render() {
    return (
      <div>
        <Section title="The order object">
          <Content>
            This is the content section.
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
