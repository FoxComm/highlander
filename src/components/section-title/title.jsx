'use strict';

import React from 'react';

export default class Title extends React.Component {
  static propTypes = {
    title: React.PropTypes.string,
    subtitle: React.PropTypes.node
  };

  render() {
    let subtitle = null;
    if (this.props.subtitle !== undefined) {
      subtitle = (
        <span className="fc-subtitle">
          &nbsp;
          { this.props.subtitle }
        </span>
      );
    }
    return (
      <h1 className="fc-title">
        { this.props.title }
        { subtitle }
      </h1>);
  }
}
