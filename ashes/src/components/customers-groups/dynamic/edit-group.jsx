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
import { PrimaryButton } from '../../common/buttons';
import { Link } from '../../link';
import { transitionTo } from 'browserHistory';

const prefixed = prefix('fc-customer-group-dynamic-edit');

const mapStateToProps = state => ({ group: state.customerGroups.dynamic.group });
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
      isValid: PropTypes.bool,
      isSaved: PropTypes.bool,
    }),
    actions: PropTypes.shape({
      reset: PropTypes.func.isRequired,
      saveGroup: PropTypes.func.isRequired,
    }).isRequired,
    fetchRegions: PropTypes.func.isRequired,
  };

  componentWillMount() {
    const { group, params } = this.props;

    if (group.id != params.groupId) {
      this.props.actions.reset();
    }
  }

  componentDidMount() {
    const { group, params, actions, fetchRegions } = this.props;

    fetchRegions();

    if (group.id != params.groupId) {
      actions.fetchGroup(params.groupId);
    }
  }

  componentDidUpdate() {
    const { id, isSaved } = this.props.group;
    if (isSaved) {
      transitionTo('customer-group', { groupId: id });
      return false;
    }

    return true;
  }

  render() {
    const { group, actions, params } = this.props;

    return (
      <NewGroupBase title="Edit Dynamic Customer Group">
        <Form onSubmit={() => actions.saveGroup()}>
          <DynamicGroupEditor />
          <div className={prefixed('form-submits')}>
            <Link to="customer-group" params={params}>Cancel</Link>
            <PrimaryButton type="submit" disabled={!group.isValid}>Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
