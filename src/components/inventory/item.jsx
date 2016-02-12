
//libs
import React, { PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav, { NavDropdown } from '../local-nav/local-nav';

export default class InventoryItem extends React.Component {
  render() {
    return <div>{this.props.children}</div>;
  }
}
