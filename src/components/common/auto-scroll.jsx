
import React from 'react';

function isElementInViewport(el) {

  const rect = el.getBoundingClientRect();

  return (
    rect.top >= 0 &&
    rect.left >= 0 &&
    rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
    rect.right <= (window.innerWidth || document.documentElement.clientWidth)
  );
}

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
