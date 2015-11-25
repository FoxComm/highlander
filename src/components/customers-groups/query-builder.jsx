import React, { PropTypes } from 'react';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import { AddButton } from '../common/buttons';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';


export default class QueryBuilder extends React.Component {

  constructor(...args) {
    super(...args);
    this.state = {
      currentCriteria: null
    };
  }

  static propTypes = {
    criterias: PropTypes.object.isRequired
  };

  static criterias = {
    all: 'all',
    none: 'none'
  };

  addCriteria() {
    console.log('add');
  }

  render () {
    return (
      <div className='fc-group-builder'>
        <Dropdown
          name='matchCriteria'
          items={QueryBuilder.criterias}
          placeholder='- Select criteria -'
          value={this.state.currentCriteria}
          onChange={ (value) => this.setState({currentCriteria: value}) }
        />
        <div className='fc-group-builder-add-criteria'>
          <AddButton onClick={this.addCriteria}/><span>Add criteria</span>
        </div>
      </div>
    );
  }
}
