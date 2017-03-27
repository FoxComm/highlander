import React, { Component } from 'react';
import { connect } from 'react-redux';
import { includes, get, flow } from 'lodash';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';

// redux
import { searchTaxonomies } from 'elastic/taxonomy';

// components
import TaxonomyWidget from './taxonomy-widget'


class TaxonomiesListWidget extends Component {

  componentDidMount() {
    this.props.fetch()
  }

  content() {
    const { taxonomies } = this.props;

    return taxonomies.map((taxonomy) => (
      <div key={taxonomy.taxonomyId}>
        <TaxonomyWidget
          context={taxonomy.context}
          taxonomyId={taxonomy.taxonomyId}
          title={taxonomy.name}
        />
      </div>
    ))
  }

  render() {
    return (
      <div>
        {this.content()}
      </div>
    )
  }

}

/*
 * Local redux store
 */
const fetch = createAsyncActions('fetchTaxonomiesList', searchTaxonomies);

const reducer = createReducer({
  [fetch.succeeded]: (state, response) => ({ ...state, taxonomies: get(response, 'result', []) }),
});

const mapState = state => ({
  taxonomies: state.taxonomies,
  fetchState: get(state.asyncActions, 'fetchTaxonomiesList', {}),
});

export default flow(
  connect(mapState, { fetch: fetch.perform }),
  makeLocalStore(addAsyncReducer(reducer), { taxonomies: [] }),
)(TaxonomiesListWidget);
