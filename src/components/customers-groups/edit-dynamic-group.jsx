//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';
import { autobind } from 'core-decorators';

//data
import criterions from '../../paragons/customer-groups/criterions';
import operators from '../../paragons/customer-groups/operators';

//components
import NewGroupBase from './new-group';
import Form from '../forms/form';
import FormField from '../forms/formfield';
import Dropdown from '../dropdown/dropdown';
import { PrimaryButton, Button } from '../common/buttons';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';

import { actions } from '../../modules/customer-groups/group';


const mapStateToProps = state => state.customerGroups.group;
const mapDispatchToProps = dispatch => ({actions: bindActionCreators(actions, dispatch)});

@connect(mapStateToProps, mapDispatchToProps)
export default class NewDynamicGroup extends React.Component {

  get nameField() {
    const {name, actions: {setName}} = this.props;

    return (
      <FormField label='Group Name'
                 labelClassName='fc-customer-group-new-title fc-customer-group-new-name'>
        <input id='nameField'
               className='fc-customer-group-new-form-name'
               name='Name'
               maxLength='255'
               type='text'
               required
               onChange={({target}) => setName(target.value)}
               value={name} />
      </FormField>
    )
  }

  get mainCondition() {
    const {query} = this.props;

    const mainConditionItems = [
      [operators.and, 'all'],
      [operators.or, 'any'],
    ];

    const value = operators.or in query ? operators.or : operators.and;

    return (
      <div className='fc-customer-group-new__match-div'>
        <span className='fc-customer-group-new__match-span'>Customers match</span>
            <span className='fc-customer-group-new__match-dropdown'>
              <Dropdown
                name='matchCriteria'
                items={mainConditionItems}
                value={value}
                onChange={this.setMainCondition}
              />
            </span>
        <span className='fc-customer-group-new__match-span'>of the following criteria:</span>
      </div>
    );
  }

  @autobind
  setMainCondition(value) {
    const {query, actions: {setQuery}} = this.props;

    //wrap with or
    if (value === operators.or) {
      setQuery({
        [operators.or]: [query]
      });
    }

    //extract from or
    if (value === operators.and) {
      setQuery(query[operators.or][0]);
    }
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
        <Form onSubmit={() => props.saveGroup()}>
          {this.nameField}
          {this.mainCondition}
          <div className='fc-customer-group-new__form-submits'>
            <Link to='customers'>Cancel</Link>
            <PrimaryButton type="submit">Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
