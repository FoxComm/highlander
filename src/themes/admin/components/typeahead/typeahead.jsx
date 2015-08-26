'use strict';

import React from 'react';
import Api from '../../lib/api';
import TypeaheadResults from './results';
import { dispatch } from '../../lib/dispatcher';

export default class Typeahead extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showResults: false,
      updating: false
    };
  }

  onItemSelected(item) {
    this.setState({
      showResults: false
    });
    dispatch(this.props.selectEvent, item);
  }

  inputKeyUp(event) {
    if (event.keyCode === 27) {
      // They hit escape
      this.setState({
        showResults: false
      });
    }
  }

  textChange(event) {
    let
      target = event.target,
      value = target.value,
      store = this.props.store;

    store.reset();
    this.setState({
      showResults: !(value === ''),
      updating: true
    });

    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => {
      Api.get(store.uri())
         .then((res) => {
           if (value !== target.value) {
             return;
           }
           this.setState({
             updating: false
           });
           store.update(res);
         })
         .catch((err) => { store.fetchError(err); });
      this.props.store.fetch();
      clearTimeout(this.timeout);
      this.timeout = null;
    }, 500);
  }

  render() {
    let labelContent = null;

    if (this.props.label) {
      labelContent = <label htmlFor={this.props.name}>{this.props.label}</label>;
    }

    return (
      <div className="typeahead">
        {labelContent}
        <div className="form-icon">
          <i className="icon-search"></i>
          <input type="text" name={this.props.name} className="control" onChange={this.textChange.bind(this)} onKeyUp={this.inputKeyUp.bind(this)} />
        </div>
        <TypeaheadResults onItemSelected={this.onItemSelected.bind(this)} selectEvent={this.props.selectEvent} component={this.props.component} store={this.props.store} showResults={this.state.showResults} updating={this.state.updating} />
      </div>
    );
  }
}

Typeahead.propTypes = {
  selectEvent: React.PropTypes.string,
  component: React.PropTypes.func,
  store: React.PropTypes.object,
  label: React.PropTypes.string,
  name: React.PropTypes.string
};

Typeahead.defaultProps = {
  name: 'typeahead'
};
