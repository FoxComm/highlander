/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import { autobind } from 'core-decorators';

// actions
import * as UserActions from '../../modules/users/details';

// components
import LocalNav, { NavDropdown } from '../local-nav/local-nav';
import WaitAnimation from '../common/wait-animation';
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import { PrimaryButton } from '../common/buttons';

type Actions = {
  fetchUser: Function,
  editUser: Function,
};

type Params = {
  userId: number,
};

type Details = {
  user: Object,
};

type Props = {
  actions: Actions,
  params: Params,
  details: Details,
  children: Element,
  fetchError: any,
  isFetching: bool,
  dispatch: Function,
};

class User extends Component {
  props: Props;

  componentDidMount() {
    const { userId } = this.props.params;

    this.props.fetchUser(userId);
  }

  get waitAnimation() {
    return <WaitAnimation/>;
  }

  get errorMessage() {
    return <div className="fc-user__empty-messages">An error occurred. Try again later.</div>;
  }

  get pageTitle(): string {
    if (this.isNew) {
      return 'New User';
    }

    return _.get(this.props, 'details.name', '');
  }

  @autobind
  handleSubmit() {
    const id = this.props.params.userId;
    const data = this.refs.form.getFormData();
    this.props.editUser(id, data);
  }

  renderChildren(): Element {
    return React.cloneElement(this.props.children, {
      ref: 'form',
      user: this.props.details
    });
  }

  renderContent() {
    const { details, params } = this.props;

    return (
      <div>
        <PageTitle title={this.pageTitle}>
          <PrimaryButton type="button" onClick={this.handleSubmit}>
            Save
          </PrimaryButton>
        </PageTitle>
        <SubNav userId={params.userId} user={details}/>
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            { this.renderChildren() }
          </div>
        </div>
      </div>
    );
  }

  render() {
    let content;

    if (this.props.failed) {
      content = this.errorMessage;
    } else if (this.props.isFetching || !this.props.details) {
      content = this.waitAnimation;
    } else {
      content = this.renderContent();
    }

    return (
      <div className="fc-user">
        {content}
      </div>
    );
  }
}

export default connect((state, props) => ({
  ...state.users.details[props.params.userId]
}), UserActions)(User);
