'use strict';

import React from 'react';
import TypeaheadResults from './results';

export default class Typeahead extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      results: [],
      showResults: false
    };
  }

  textChange(event) {
    let value = event.target.value;
    if (value === '') {
      this.setState({
        showResults: false,
        results: []
      });
      return;
    }

    clearTimeout(this.timeout);
    this.timeout = setTimeout(() => {
      console.log(value);
      this.setState({
        showResults: true
      });
      this.props.store.fetch();
      clearTimeout(this.timeout);
      this.timeout = null;
    }, 300);
  }

  render() {
    return (
      <div>
        <input type="text" className="control" onChange={this.textChange.bind(this)} />
        { this.state.showResults ? <TypeaheadResults selectEvent={this.props.selectEvent} component={this.props.component} store={this.props.store}/> : null }
      </div>
    );
  }
}

Typeahead.propTypes = {
  selectEvent: React.PropTypes.string,
  component: React.PropTypes.element,
  path: React.PropTypes.string,
  store: React.PropTypes.object
};
