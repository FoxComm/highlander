import React, { PropTypes, Component } from 'react';
import { Link, IndexLink } from '../link';
//import TitleBlock from './title-block';
import { connect } from 'react-redux';
import * as UserActions from '../../modules/users/details';
import LocalNav, { NavDropdown } from '../local-nav/local-nav';
import WaitAnimation from '../common/wait-animation';

@connect((state, props) => ({
  ...state.users.details[props.params.userId]
}), UserActions)
export default class User extends Component {

  componentDidMount() {
    const { userId } = this.props.params;

    this.props.fetchUser(userId);
  }

  renderChildren() {
    return React.Children.map(this.props.children, child => {
      return React.cloneElement(child, {
        entity: this.props.details
      });
    });
  }

  render() {
    let content;

    if (this.props.failed) {
      content = this.errorMessage;
    } else if (this.props.isFetching || !this.props.details) {
      content = this.waitAnimation;
    } else {
      content = this.content;
    }

    return (
      <div className="fc-user">
        {content}
      </div>
    );
  }

  get waitAnimation() {
    return <WaitAnimation/>;
  }

  get errorMessage() {
    return <div className="fc-user__empty-messages">An error occurred. Try again later.</div>;
  }

  get content() {
    const { details, params } = this.props;

    return (
      <div>
        <div className="fc-grid">

        </div>
        <LocalNav gutter={true}>
          <IndexLink to="user-details" params={params}>Details</IndexLink>
        </LocalNav>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            { this.renderChildren() }
          </div>
        </div>
      </div>
    );
  }
}
