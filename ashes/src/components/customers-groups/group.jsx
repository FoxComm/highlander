/* @flow */

//libs
import get from 'lodash/get';
import React from 'react';
import { connect } from 'react-redux';

import { reset, fetchGroup } from '../../modules/customer-groups/dynamic/group';

//components
import WaitAnimation from '../common/wait-animation';
import DynamicGroup from './dynamic/group';

type Props = {
  group: TCustomerGroup;
  isLoading: boolean;
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
    if (this.props.isLoading) {
      return <WaitAnimation />;
    }

    return <DynamicGroup group={this.props.group} />;
  };
}

const mapStateToProps = state => ({
  isLoading: get(state, ['asyncActions', 'fetchCustomerGroup', 'inProgress'], false),
  group: get(state, ['customerGroups', 'dynamic', 'group']),
});

export default connect(mapStateToProps, { reset, fetchGroup })(Group);
