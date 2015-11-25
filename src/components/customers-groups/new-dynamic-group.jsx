import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import { PrimaryButton, DefaultButton } from '../common/buttons';
import { Link } from '../link';
import QueryBuilder from './query-builder';
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';


export default class NewDynamicGroup extends React.Component {

  constructor(props, context) {
    super(props, context);
    this.state = {
      name: '',
      matchCriteria: 'all'
    };
  }

  static propTypes = {
    customerCount: PropTypes.number
  };

  static criterias = [
    {
      term: 'Blacklisted status',
      type: 'bool'
    },
    {
      term: 'Region',
      type: 'enum',
      suggestions: []
    },
    {
      term: 'Total Sales',
      type: 'number'
    },
    {
      term: 'Date Joined',
      type: 'date'
    }
  ];

  render () {
    const mainMatchStatuses = {
      all: 'all',
      none: 'none'
    };
    return (
      <div className='fc-group-new'>
        <div className='fc-grid'>
          <header className='fc-customer-form-header fc-col-md-1-1'>
            <h1 className='fc-title'>
              New Dynamic Customer Group
            </h1>
            <Link className='fc-group-new-or gc-group-new-title' to='groups-new-manual'>or create a manual group</Link>
          </header>
          <article className='fc-col-md-1-1'>
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
              <QueryBuilder criterias={NewDynamicGroup.criterias}/>
              <div className='fc-group-new-title fc-group-new-count-title'>Customer Count:</div>
              <div className='fc-group-new-count'>{ this.props.customerCount }</div>
              <div className='fc-group-new-form-submits'>
                <Link to='customers'>Cancel</Link>
                <DefaultButton>Make Manual Group</DefaultButton>
                <PrimaryButton>Save Dynamic Group</PrimaryButton>
              </div>
            </Form>
          </article>
        </div>
      </div>
    );
  }
}
