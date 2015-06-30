'use strict';

import React from 'react';
import Api from '../../lib/api';
import TypeaheadResults from './results';

export default class Typeahead extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showResults: false
    };
  }

  textChange(event) {
    let
      target = event.target,
      value = target.value,
      store = this.props.store;

    store.reset();
    this.setState({
      showResults: !(value === '')
    });

    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => {
      console.log(value);
      Api.get(store.uri())
         .then((res) => {
           if (value !== target.value) {
             return;
           }
           store.update(res);
         })
         .catch((err) => { store.fetchError(err); });
      this.props.store.fetch();
      clearTimeout(this.timeout);
      this.timeout = null;
    }, 500);
  }

  render() {
    return (
      <div className="typeahead">
        <input type="text" className="control" onChange={this.textChange.bind(this)} />
        <TypeaheadResults selectEvent={this.props.selectEvent} component={this.props.component} store={this.props.store} showResults={this.state.showResults} />
      </div>
    );
  }
}

Typeahead.propTypes = {
  selectEvent: React.PropTypes.string,
  component: React.PropTypes.func,
  path: React.PropTypes.string,
  store: React.PropTypes.object
};
