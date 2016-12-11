/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import { IndexLink } from 'components/link';
import LocalNav from 'components/local-nav/local-nav';

export default class SubNav extends Component {
  render() {
    return (
      <LocalNav>
        <IndexLink to="shipping-methods">List</IndexLink>
      </LocalNav>
    );
  }
}

