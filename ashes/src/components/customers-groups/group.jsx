/* @flow */

//libs
import get from 'lodash/get';
import React from 'react';
import { connect } from 'react-redux';

import { reset, fetchGroup } from '../../modules/customer-groups/group';

//components
import WaitAnimation from '../common/wait-animation';
import DynamicGroup from './dynamic/group';
import Error from 'components/errors/error';

type Props = {
  group: TCustomerGroup;
  inProgress: boolean;
  err: Object;
  reset: () => void;
  fetchGroup: (id: string) => Promise;
  params: {
    groupId: string;
  };
};

class Group extends React.Component {
  props: Props;

  componentWillMount() {
    const { group, reset, params: { groupId } } = this.props;

    if (groupId != group.id) reset();
  }

  componentDidMount() {
    const { group, fetchGroup, params: { groupId } } = this.props;

    if (groupId != group.id) fetchGroup(groupId);
  }

  render() {
    const { group, inProgress, err } = this.props;

    if (err) {
      return <Error err={err} />;
    }

    if (inProgress || !group.id) {
      return <div><WaitAnimation /></div>;
    }

    return <DynamicGroup group={group} />;
  };
}

const mapStateToProps = state => ({
  inProgress: get(state, 'asyncActions.fetchCustomerGroup.inProgress', false),
  err: get(state, ['asyncActions', 'fetchCustomerGroup', 'err'], false),
  group: get(state, ['customerGroups', 'details', 'group']),
});

export default connect(mapStateToProps, { reset, fetchGroup })(Group);
