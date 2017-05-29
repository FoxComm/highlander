/**
 * @flow
 */

// libs
import React, { Component } from 'react';

// components
import { Link, IndexLink } from 'components/link';
import PageNav from 'components/core/page-nav';

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
      <PageNav>
        <IndexLink to="user-form" params={params}>Settings</IndexLink>
        {this.props.userId !== 'new' && <Link to="user-activity-trail" params={params}>Activity Trail</Link>}
      </PageNav>
    );
  }
}
