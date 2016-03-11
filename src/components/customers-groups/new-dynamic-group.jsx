//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

//data
import criterions from '../../modules/customer-groups/criterions';
import { actions } from '../../modules/customer-groups/group';
import operators from '../../paragons/customer-groups/operators';

//components
import NewGroupBase from './new-group';
import DynamicGroupEditor from './dynamic-group-editor';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import Dropdown from '../dropdown/dropdown';
import { PrimaryButton, Button } from '../common/buttons';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';


const mapStateToProps = state => ({group: state.customerGroups.group});
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class NewDynamicGroup extends React.Component {

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

    if (console.debug) {
      console.group('new dynamic group render');
      for (const name in props) {
        console.debug(name, props[name]);
      }
      console.groupEnd();
    }

    return (
      <NewGroupBase title="New Dynamic Customer Group"
                    alternative={{
                      id: 'new-manual-group',
                      title: 'manual group',
                    }}>
        <Form onSubmit={() => props.actions.saveGroup()}>
          <DynamicGroupEditor />
          <div className='fc-customer-group-new__form-submits'>
            <Link to='customers'>Cancel</Link>
            <PrimaryButton type="submit">Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
