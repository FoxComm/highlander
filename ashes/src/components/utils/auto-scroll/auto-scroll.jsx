/* @flow */

// libs
import React, { Component } from 'react';
import { isElementInViewport } from 'lib/dom-utils';

export default class AutoScroll extends Component {
  _node: Element;

  componentDidMount() {
    if (!isElementInViewport(this._node)) {
      this._node.scrollIntoView();
    }
  }

  render() {
    return (
      <div ref={n => this._node = n} />
    );
  }
}
