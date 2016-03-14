//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

//data
import { actions } from '../../modules/customer-groups/group';

//helpers
import { prefix } from '../../lib/text-utils';

//components
import NewGroupBase from './new-group';
import DynamicGroupEditor from './dynamic-group-editor';
import Form from '../forms/form';
import { PrimaryButton, Button } from '../common/buttons';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';


const prefixed = prefix('fc-customer-group-dynamic-edit__');

const mapStateToProps = state => ({group: state.customerGroups.group});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class NewDynamicGroup extends React.Component {

  static props = {
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

  componentDidUpdate() {
    const {id} = this.props.group;
    if (id) {
      transitionTo(this.context.history, 'group', {groupId: id});
      return false;
    }

    return true;
  }

  render() {
    const {props} = this;

    return (
      <NewGroupBase title="New Dynamic Customer Group"
                    alternative={{
                      id: 'new-manual-group',
                      title: 'manual group',
                    }}>
        <Form onSubmit={() => props.actions.saveGroup()}>
          <DynamicGroupEditor />
          <div className={prefixed('form-submit')}>
            <Link to="customer-groups">Cancel</Link>
            <PrimaryButton type="submit">Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
