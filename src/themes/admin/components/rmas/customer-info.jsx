'use strict';

import React from 'react';

export default class CustomerInfo extends React.Component {
  render() {
    return (
      <div className="fc-rma-summary fc-content-box">
        <header className="header">Message for Customer</header>
        <article>
          {this.props.rma.customerMessage}
        </article>
      </div>
    );
  }
}

CustomerInfo.propTypes = {
  rma: React.PropTypes.object
};
