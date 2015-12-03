import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import { PrimaryButton, Button } from '../common/buttons';
import { Link } from '../link';
import NewGroupBase from './new-group-base.jsx';
import QueryBuilder from './query-builder';
import { connect } from 'react-redux';
import { assoc } from 'sprout-data';

export default class NewDynamicGroup extends React.Component {

  constructor(props, context) {
    super(props, context);
    this.state = {
      name: '',
      matchCriteria: 'all'
    };
  }

  render() {
    const mainMatchStatuses = {
      all: 'all',
      none: 'none'
    };

    return (
      <NewGroupBase title='New Dynamic Customer Group'
                  alternativeId='groups-new-manual'
                  alternativeTitle='manual group'>
        <Form>
          <FormField label='Group Name'
                     labelClassName='fc-group-new-title fc-group-new-name'>
            <input id='nameField'
                   className='fc-group-new-form-name'
                   name='Name'
                   maxLength='255'
                   type='text'
                   required
                   onChange={ ({target}) => this.setState({name: target.value}) }
                   value={ this.state.name } />
          </FormField>
          <div className='fc-group-new-match-div'>
            <span className='fc-group-new-match-span'>Customers match</span>
            <span className='fc-group-new-match-dropdown'>
              <Dropdown
                name='matchCriteria'
                items={mainMatchStatuses}
                value={this.state.matchCriteria}
                onChange={ (value) => this.setState({matchCriteria: value}) }
                  />
            </span>
            <span className='fc-group-new-match-span'>of the following criteria:</span>
          </div>
          <QueryBuilder/>
          <div className='fc-group-new-form-submits'>
            <Link to='customers'>Cancel</Link>
            <Button>Make Manual Group</Button>
            <PrimaryButton type="submit">Save Dynamic Group</PrimaryButton>
          </div>
        </Form>
      </NewGroupBase>
    );
  }
}
