/**
 * @flow
 */

// libs
import React, { Component, PropTypes } from 'react';

// components
import { Link, IndexLink } from '../link';
import LocalNav from '../local-nav/local-nav';

// types
//import type { User } from '../../modules/users/details';

type Props = {
  userId: string,
  //user: ?User,
  user: Object,
  context: string
};

export default class SubNav extends Component<void, Props, void> {
  static propTypes = {
    userId: PropTypes.string.isRequired,
    user: PropTypes.object,
    //context: PropTypes.string
  };

  render() {
    const params = {
      userId: this.props.userId,
      user: this.props.user,
      //context: this.props.context
    };

    return (
      <LocalNav>
        <IndexLink to="user-form" params={params}>Settings</IndexLink>
        <Link to="user-activity-trail" params={params}>Activity Trail</Link>
      </LocalNav>
    );
  }
}
