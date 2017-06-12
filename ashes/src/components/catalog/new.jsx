/* @flow */

import React, { Component } from 'react';
import NewCatalogForm from './new-form';

type State = {
  name: string,
  defaultLanguage: string,
  site: string,
};

class NewCatalog extends Component {
  state: State = {
    name: '',
    defaultLanguage: '',
    site: '',
  };

  handleChange = (field: string, value: any) => {
    this.setState({ [field]: value });
  };
    
  render() {
    const { name, defaultLanguage, site } = this.state;
    
    return (
      <NewCatalogForm
        name={name}
        defaultLanguage={defaultLanguage}
        site={site}
        onChange={this.handleChange}
      />
    )
  }
}

export default NewCatalog;
