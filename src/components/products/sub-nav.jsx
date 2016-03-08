/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// types
import type { DetailsParams } from './types';

export default class SubNav extends Component<void, DetailsParams, void> {
  render() {
    const params = { 
      productId: this.props.productId,
      product: this.props.product,
    };

    return (
      <LocalNav>
        <IndexLink to="product-details" param={params}>Details</IndexLink>
        <a href="">Notes</a>
        <a href="">ActivityTrail</a>
      </LocalNav>
    );
  }
}
