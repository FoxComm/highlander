/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

export default class SubNav extends Component {
  render(): Element {
    return (
      <LocalNav>
        <IndexLink to="business-profile-details">Details</IndexLink>
      </LocalNav>
    );
  }
}