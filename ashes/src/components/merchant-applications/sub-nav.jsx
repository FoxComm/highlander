/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import { IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';

export default class SubNav extends Component {
  render() {
    return (
      <PageNav>
        <IndexLink to="applications">List</IndexLink>
      </PageNav>
    );
  }
}
