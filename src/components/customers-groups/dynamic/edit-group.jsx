//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

//data
import { actions } from '../../../modules/customer-groups/dynamic/group';
import { fetchRegions } from '../../../modules/regions';

//helpers
import { prefix } from '../../../lib/text-utils';

//components
import NewGroupBase from './../new-group';
import DynamicGroupEditor from './group-editor';
import Form from '../../forms/form';
import { PrimaryButton, Button } from '../../common/buttons';
import { Link } from '../../link';
import { transitionTo } from '../../../route-helpers';


const prefixed = prefix('fc-customer-group-dynamic-edit__');

const mapStateToProps = state => ({group: state.customerGroups.dynamic.group});
const mapDispatchToProps = dispatch => ({
  actions: bindActionCreators(actions, dispatch),
  fetchRegions: () => dispatch(fetchRegions()),
});

@connect(mapStateToProps, mapDispatchToProps)
export default class EditDynamicGroup extends React.Component {

  static propTypes = {
    params: PropTypes.shape({
      groupId: PropTypes.string,
    }),
    group: PropTypes.shape({
      id: PropTypes.number,
    }),
    actions: PropTypes.shape({
      reset: PropTypes.func.isRequired,
      saveGroup: PropTypes.func.isRequired,
    }).isRequired,
  };

  static contextTypes = {
    history: PropTypes.object.isRequired
  };

  componentWillMount() {
    this.props.actions.reset();
  }

  componentDidMount() {
    const {params, actions, fetchRegions} = this.props;
    actions.fetchGroup(params.groupId);
    fetchRegions();
  }

  componentDidUpdate() {
    const {id, saved} = this.props.group;
    if (saved) {
      transitionTo(this.context.history, 'customer-group', {groupId: id});
      return false;
    }

    return true;
  }

  render() {
    const {group, actions} = this.props;

    return (
      <NewGroupBase title="Edit Dynamic Customer Group">
        <Form onSubmit={() => actions.saveGroup()}>
          <DynamicGroupEditor />
          <div className={prefixed('form-submits')}>
            <Link to="customer-group" params={{groupId: group.id}}>Cancel</Link>
            <PrimaryButton type="submit">Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
