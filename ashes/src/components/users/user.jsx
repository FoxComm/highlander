/* @flow */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { transitionTo } from 'browserHistory';

// actions
import * as userActions from 'modules/users/details';
import { requestPasswordReset, clearResetPasswordState } from 'modules/user';

// components
import Spinner from 'components/core/spinner';
import { PageTitle } from '../section-title';
import SubNav from './sub-nav';
import { Button, PrimaryButton } from 'components/core/button';

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
  isFetching: boolean,
  fetchUser: Function,
  userNew: Function,
  createUser: Function,
  updateUser: Function,
  requestPasswordReset: (email: string) => Promise<*>,
  clearResetPasswordState: () => void,
  restoreState: {
    err?: any,
    inProgress?: boolean,
  },
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
      requestPasswordReset: this.props.requestPasswordReset,
      clearResetState: this.props.clearResetPasswordState,
      restoreState: this.props.restoreState,
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
    this.props.createUser(this.state.user).then(payload => {
      transitionTo('user', { userId: payload.id });
    });
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
        <SubNav userId={params.userId} user={details} />
        <div className="fc-grid">
          <div className="fc-col-md-1-1">
            {this.renderChildren()}
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
      content = <Spinner />;
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

function mapState(state) {
  return {
    details: state.users.details,
    isFetching: _.get(state.asyncActions, 'getUser.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getUser.err', null),
    restoreState: _.get(state.asyncActions, 'requestPasswordReset', {}),
  };
}

export default connect(mapState, {
  ...userActions,
  requestPasswordReset,
  clearResetPasswordState,
})(User);
