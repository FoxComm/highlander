import React, { Component } from 'react';
import { connect } from 'react-redux';
import { includes, get, flow, findIndex, find, remove } from 'lodash';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';
import { dissoc } from 'sprout-data';
import { autobind } from 'core-decorators';

// redux
import { searchTaxonomies } from 'elastic/taxonomy';

// components
import TaxonomyWidget from './taxonomy-widget'


class TaxonomiesListWidget extends Component {

  componentDidMount() {
    this.props.fetch()
  }

  @autobind
  handleDelete(taxonomyId, taxonId) {
    const { handleChange, addedTaxons } = this.props;

    const taxonomyIndex = findIndex(addedTaxons, (taxonomy) => {
      return taxonomy.taxonomyId === taxonomyId
    });

    const taxonomy = find(addedTaxons, (taxonomy) => {
      return taxonomy.taxonomyId === taxonomyId
    });

    remove(taxonomy.taxons, (taxon) => {
     return taxon.id === taxonId
    });

    addedTaxons[taxonomyIndex].taxons = taxonomy.taxons;

    handleChange(addedTaxons)
  }

  content() {
    const { taxonomies, addedTaxons, productId} = this.props;

    return taxonomies.map((taxonomy) => {

      let addedTaxonsList = [];

      addedTaxons.map((taxon) => {
        if (taxon.taxonomyId === taxonomy.taxonomyId) {
          addedTaxonsList.push(taxon)
        }
      });

     return (
       <div key={taxonomy.taxonomyId}>
          <TaxonomyWidget
            addedTaxons={addedTaxonsList}
            productId={productId}
            context={taxonomy.context}
            taxonomyId={taxonomy.taxonomyId}
            title={taxonomy.name}
            handleDelete={this.handleDelete}
          />
      </div>
     )
    })
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
