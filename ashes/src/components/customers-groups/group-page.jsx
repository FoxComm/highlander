/* @flow */

//libs
import get from 'lodash/get';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import { push } from 'react-router-redux';

import { reset, fetchGroup, archiveGroup, clearArchiveErrors } from 'modules/customer-groups/details/group';

//components
import ArchiveActionsSection from 'components/archive-actions/archive-actions';
import Error from 'components/errors/error';
import WaitAnimation from 'components/common/wait-animation';
import GroupDetails from './group-details';

type Props = {
  group: TCustomerGroup,
  fetched: boolean,
  err: Object,
  archiveState: AsyncState,
  reset: () => void,
  clearArchiveErrors: () => Promise,
  fetchGroup: (id: number) => Promise,
  archiveGroup: (id: number) => Promise,
  push: (location: Object) => void,
  params: {
    groupId: string,
  },
};

class GroupPage extends Component {
  props: Props;

  componentWillMount() {
    if (!this.isRequestedGroup) {
      this.props.reset();
    }
  }

  componentDidMount() {
    if (!this.isRequestedGroup) {
      this.props.fetchGroup(parseInt(this.props.params.groupId, 10));
    }
  }

  get isRequestedGroup() {
    return this.props.group.id == parseInt(this.props.params.groupId, 10);
  }

  @autobind
  archiveGroup() {
    this.props.archiveGroup(this.props.group.id)
      .then(() => {
        this.props.reset();
        this.props.push({ name: 'groups' });
      });
  }

  render() {
    const { group, err } = this.props;

    if (err) {
      return <Error err={err} />;
    }

    if (!this.isRequestedGroup) {
      return <div><WaitAnimation /></div>;
    }

    return (
      <div>
        <GroupDetails group={group} />

        <ArchiveActionsSection
          type="Group"
          title={group.name}
          archive={this.archiveGroup}
          archiveState={this.props.archiveState}
          clearArchiveErrors={this.props.clearArchiveErrors}
        />
      </div>
    );
  };
}

const mapStateToProps = state => ({
  fetched: get(state, 'asyncActions.fetchCustomerGroup.finished', false),
  err: get(state, ['asyncActions', 'fetchCustomerGroup', 'err'], false),
  group: get(state, ['customerGroups', 'details', 'group']),
  archiveState: get(state, ['asyncActions', 'archiveCustomerGroup'], {}),
});

export default connect(mapStateToProps, { reset, fetchGroup, archiveGroup, clearArchiveErrors, push })(GroupPage);
