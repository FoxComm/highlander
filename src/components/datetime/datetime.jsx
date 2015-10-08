'use strict';

import React from 'react';
import moment from 'moment';

export default class DateTime extends React.Component {
  static propTypes = {
    value: React.PropTypes.oneOfType([
      React.PropTypes.string,
      React.PropTypes.object
    ])
  };

  render() {
    return (
      <time dateTime={this.props.value}>
        {moment(this.props.value).format('MM/DD/YYYY HH:mm:ss')}
      </time>
    );
  }
}
