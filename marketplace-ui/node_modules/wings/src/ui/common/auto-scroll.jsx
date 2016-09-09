
import React from 'react';
import { isElementInViewport } from '../../dom-utils';

export default class AutoScroll extends React.Component {
  componentDidMount() {
    if (!isElementInViewport(this._node)) {
      this._node.scrollIntoView();
    }
  }

  render() {
    return (
      <div ref={(c) => this._node = c}></div>
    );
  }
}
