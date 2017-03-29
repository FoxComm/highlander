// @flow

// libs
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { flow, get, find } from 'lodash';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';

// redux
import { searchTaxonomies } from 'elastic/taxonomy';

// components
import WaitAnimation from 'components/common/wait-animation';
import TaxonomyWidget from './taxonomy-widget';

// styles
import styles from './taxonomies-list-widget.css';

type Props = {
  productId: number,
  linkedTaxonomies: Array<LinkedTaxonomy>,
  onChange: (taxons: Array<LinkedTaxonomy>) => any,
  systemTaxonomies: Array<TaxonomyResult>,
  fetch: () => Promise<*>,
  fetchState: AsyncState,
};

class TaxonomiesListWidget extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetch();
  }

  render() {
    const { systemTaxonomies, linkedTaxonomies, productId, fetchState } = this.props;

    if (!systemTaxonomies || fetchState.inProgress && !fetchState.err) {
      return <WaitAnimation className={styles.waiting} />;
    }

    return (
      <div>
        {systemTaxonomies.map((taxonomy: TaxonomyResult) => {
          const linkedTaxonomy = find(linkedTaxonomies, linked => linked.taxonomyId === taxonomy.taxonomyId);

          return (
            <div key={taxonomy.taxonomyId}>
              <TaxonomyWidget
                linkedTaxonomy={linkedTaxonomy}
                productId={productId}
                context={taxonomy.context}
                taxonomyId={taxonomy.taxonomyId}
                title={taxonomy.name}
                onChange={this.props.onChange}
              />
            </div>
          );
        })}
      </div>
    );
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
  systemTaxonomies: state.taxonomies,
  fetchState: get(state.asyncActions, 'fetchTaxonomiesList', {}),
});

export default flow(
  connect(mapState, { fetch: fetch.perform }),
  makeLocalStore(addAsyncReducer(reducer), { taxonomies: [] }),
)(TaxonomiesListWidget);
