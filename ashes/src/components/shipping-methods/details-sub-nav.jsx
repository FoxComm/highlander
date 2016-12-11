/**
 * @flow
 */

// libs
import React, { Component, PropTypes, Element } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

type Props = {
  shippingMethodId: number,
};

export default class DetailsSubNav extends Component {
  props: Props;

  render(): Element {
    const params = { shippingMethodId: this.props.shippingMethodId };
    return (
      <LocalNav>
        <IndexLink to="shipping-method-details" params={params}>Details</IndexLink>
      </LocalNav>
    );
  }
}