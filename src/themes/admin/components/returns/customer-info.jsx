'use strict';

import React from 'react';

export default class CustomerInfo extends React.Component {
  render() {
    let retrn = this.props.return;

    return (
      <div className="fc-return-summary fc-content-box">
        <header className="header">Message for Customer</header>
        <article>
          {retrn.customerMessage}
        </article>
      </div>
    );
  }
}

CustomerInfo.propTypes = {
  return: React.PropTypes.object
};
