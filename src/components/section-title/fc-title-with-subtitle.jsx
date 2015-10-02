'use strict';

import React from 'react';

export default class FCTitleWithSubtitle extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <h1 className="fc-title">
        { this.props.title }
        &nbsp;
        <span className="fc-subtitle">
          { this.props.subtitle }
        </span>
      </h1>
    );
  }
}

FCTitleWithSubtitle.propTypes = {
  title: React.PropTypes.string,
  subtitle: React.PropTypes.string
};


