/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';

// actions
import * as UserActions from 'modules/users/details';

// components
import WaitAnimation from '../common/wait-animation';
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import { Button, PrimaryButton } from '../common/buttons';

type Params = {
  userId: number,
};

type Details = {
  user: Object,
};

type Props = {
  params: Params,
  details: Details,
  children: Element<*>,
  fetchError: any,
  isFetching: bool,
  fetchUser: Function,
  userNew: Function,
  createUser: Function,
  updateUser: Function,
};

type State = {
  user: Object,
};

class User extends Component {
  props: Props;

  state: State = {
    ...this.props.details,
  };

  componentDidMount() {
    const { userId } = this.props.params;

    this.props.fetchUser(userId);
  }

  componentWillReceiveProps(nextProps) {
    if (!nextProps.isFetching && !nextProps.fetchError) {
      const { details } = nextProps;
      if (!details) return;

      this.setState({ ...details });
    }
  }

  get isNew(): boolean {
    return this.props.params.userId === 'new';
  }

  get errorMessage() {
    return <div className="fc-user__empty-messages">An error occurred. Try again later.</div>;
  }

  get activityEntity() {
    return {
      // TODO change this to 'user'
      entityType: 'account',
      entityId: this.props.params.userId,
    };
  }

  @autobind
  handleFormChange(user) {
    this.setState({ user });
  }

  @autobind
  handleSubmit() {
    this.props.updateUser(this.state.user);
  }

  renderChildren() {
    return React.cloneElement(this.props.children, {
      user: this.state.user,
      onChange: this.handleFormChange,
      isNew: this.isNew,
      entity: this.activityEntity,
    });
  }

  renderUserTitle() {
    const title = _.get(this.props, 'details.user.name', '');
    return (
      <PageTitle title={title}>
        <PrimaryButton type="button" onClick={this.handleSubmit}>
          Save
        </PrimaryButton>
      </PageTitle>
    );
  }

  @autobind
  handleNewUserSubmit() {
    this.props.createUser(this.state.user).then(({ payload }) => {
        transitionTo('user', {userId: payload.id});
      }
    );
  }

  renderNewUserTitle() {
    return (
      <PageTitle title="New User">
        <Button type="button" onClick={() => transitionTo('users')}>Cancel</Button>
        <PrimaryButton type="button" onClick={this.handleNewUserSubmit}>
          Invite User
        </PrimaryButton>
      </PageTitle>
    );
  }

  renderContent() {
    const { details, params } = this.props;

    return (
      <div>
        {this.isNew ? this.renderNewUserTitle() : this.renderUserTitle()}
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

    if (this.props.fetchError) {
      content = this.errorMessage;
    } else if (this.props.isFetching || !this.state.user) {
      content = <WaitAnimation/>;
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

export default connect(
  state => ({
    details: state.users.details,
    isFetching: _.get(state.asyncActions, 'getUser.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getUser.err', null),
  }),
  UserActions
)(User);
