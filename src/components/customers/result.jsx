'use strict';

import React from 'react';

export default class CustomerResult extends React.Component {
  render() {
    let customer = this.props.model;
    return (
      <div>{customer.firstName} {customer.lastName}</div>
    );
  }
}

CustomerResult.propTypes = {
  model: React.PropTypes.object
};
