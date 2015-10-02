'use strict';

import React from 'react';

export default class FCTitle extends React.Component {
  constructor(props) {
    super(props);
  }

  render() {
    return (
      <h1 className="fc-title">
        { this.props.title }
      </h1>
    );
  }
}

FCTitle.propTypes = {
  title: React.PropTypes.string
};
