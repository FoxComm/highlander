'use strict';

import React from 'react';

export default class Title extends React.Component {
  render() {
    let titleMarkup = null;
    if (this.props.subtitle !== undefined) {
      titleMarkup = (
        <h1 className="fc-title">
          { this.props.title }
          &nbsp;
          <span className="fc-subtitle">
            { this.props.subtitle }
          </span>
        </h1>
      );
    } else {
      titleMarkup = (
        <h1 className="fc-title">
          { this.props.title }
        </h1>
      );
    }
    return titleMarkup;
  }
}

Title.propTypes = {
  title: React.PropTypes.string,
  subtitle: React.PropTypes.string
};


