/* @flow */

import React, { Component } from 'react';

import type { HTMLElement } from '../../core/types';

class Home extends Component {
  get greeting(): HTMLElement {
    return <h1>Hello from Home Page!</h1>;
  }

  render(): HTMLElement {
    return (
      <div>
        {this.greeting}
      </div>
    );
  }
}

export default Home;
