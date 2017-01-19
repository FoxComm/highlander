/* @flow */

//libs
import get from 'lodash/get';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import { reset, fetchGroup, deleteGroup, clearDeleteErrors } from 'modules/customer-groups/group';

//components
import ArchiveActionsSection from 'components/archive-actions/archive-actions';
import Error from 'components/errors/error';
import WaitAnimation from 'components/common/wait-animation';
import DynamicGroup from './dynamic-group';

type Props = {
  group: TCustomerGroup;
  inProgress: boolean;
  err: Object;
  deleteState: AsyncState;
  reset: () => void;
  clearDeleteErrors: () => Promise;
  fetchGroup: (id: string) => Promise;
  deleteGroup: (id: string) => Promise;
  push: (location: Object) => void;
  params: {
    groupId: string;
  };
};

class GroupPage extends Component {
  props: Props;

  componentWillMount() {
    const { group, reset, params: { groupId } } = this.props;

    if (groupId != group.id) reset();
  }

  componentDidMount() {
    const { group, fetchGroup, params: { groupId } } = this.props;

    if (groupId != group.id) fetchGroup(groupId);
  }

  @autobind
  deleteGroup() {
    this.props.deleteGroup(this.props.group.id)
      .then(() => {
        this.props.reset();
        this.props.push({ name: 'groups' });
      });
  }

  render() {
    const { group, inProgress, err } = this.props;

    if (err) {
      return <Error err={err} />;
    }

    if (inProgress || !group.id) {
      return <div><WaitAnimation /></div>;
    }

    return (
      <div>
        <DynamicGroup group={group} />

        <ArchiveActionsSection
          type="Group"
          title={group.name}
          archive={this.deleteGroup}
          archiveState={this.props.deleteState}
          clearArchiveErrors={this.props.clearDeleteErrors}
        />
      </div>
    );
  };
}

const mapStateToProps = state => ({
  inProgress: get(state, 'asyncActions.fetchCustomerGroup.inProgress', false),
  err: get(state, ['asyncActions', 'fetchCustomerGroup', 'err'], false),
  group: get(state, ['customerGroups', 'details', 'group']),
  deleteState: get(state, ['asyncActions', 'deleteCustomerGroup'], {}),
});

export default connect(mapStateToProps, { reset, fetchGroup, deleteGroup, clearDeleteErrors, push })(GroupPage);
