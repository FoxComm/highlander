/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

type Props = {
  userId: string|number,
  user: Object,
};

export default class SubNav extends Component {
  props: Props;

  render() {
    const params = {
      userId: this.props.userId,
      user: this.props.user,
    };

    return (
      <LocalNav>
        <IndexLink to="user-form" params={params}>Settings</IndexLink>
        <Link to="user-activity-trail" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  }
}
