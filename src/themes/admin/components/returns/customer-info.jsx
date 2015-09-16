'use strict';

import React from 'react';

export default class CustomerInfo extends React.Component {
  render() {
    return (
      <div className="fc-return-summary fc-content-box">
        <header className="header">Message for Customer</header>
        <article>
          {this.props.return.customerMessage}
        </article>
      </div>
    );
  }
}

CustomerInfo.propTypes = {
  return: React.PropTypes.object
};
