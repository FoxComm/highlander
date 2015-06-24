'use strict';

import React from 'react';

export default class OrderLineItems extends React.Component {
  render() {
    return (
      <section>
        <header>Items</header>
      </section>
    );
  }
}

OrderLineItems.propTypes = {
  order: React.PropTypes.object
};
