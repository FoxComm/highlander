/* @flow */

import React, { Component } from 'react';
import { connect } from 'react-redux';
import { includes, get, flow } from 'lodash';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';
import { createAsyncActions } from '@foxcomm/wings';

import { anyPermitted, isPermitted } from 'lib/claims';
import { frn, readAction } from 'lib/frn';
import { searchTaxonomies } from 'elastic/taxonomy';

import NavigationItem from '../navigation-item';
import { IndexLink, Link } from 'components/link';
import WaitAnimation from 'components/common/wait-animation';

// styles
import styles from './entries.css';

const taxonomyClaims = readAction(frn.merch.taxonomy);

type Props = TMenuEntry & {
  taxonomies: Array<TaxonomyResult>,
  fetchState: AsyncState,
  archiveState: AsyncState,
  createState: AsyncState,
  updateState: AsyncState,
  fetch: () => Promise<*>,
  currentParams: {
    taxonomyId: string,
  }
};

class MerchandisingEntry extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetch();
  }

  componentWillReceiveProps(nextProps: Props) {
    const created = this.props.createState.inProgress && !nextProps.createState.inProgress;
    const updated = this.props.updateState.inProgress && !nextProps.updateState.inProgress;
    const archived = this.props.archiveState.inProgress && !nextProps.archiveState.inProgress;

    if (created || updated || archived) {
      this.props.fetch();
    }
  }

  get taxonomiesList() {
    const { claims, routes, taxonomies, currentParams, fetchState } = this.props;

    if (!taxonomies || fetchState.inProgress) {
      return <div><WaitAnimation /></div>;
    }

    return taxonomies.map((taxonomy: TaxonomyResult) => {
      const linkParams = {
        context: taxonomy.context,
        taxonomyId: taxonomy.taxonomyId
      };

      return (
        <li key={`${taxonomy.taxonomyId}`}>
          <NavigationItem
            to="taxonomy"
            linkParams={linkParams}
            icon="taxonomies"
            title={`${taxonomy.name}`}
            routes={routes}
            actualClaims={claims}
            expectedClaims={taxonomyClaims}
            forceActive={taxonomy.taxonomyId.toString() === currentParams.taxonomyId}
          />
        </li>
      );
    });
  }

  render() {
    const { claims, routes, currentParams } = this.props;
    const allClaims = taxonomyClaims;

    if (!anyPermitted(allClaims, claims)) {
      return null;
    }

    const routeNames = routes.map(route => route.name);
    const manageRoute = includes(routeNames, 'taxonomies') && !currentParams.taxonomyId ||
      includes(routeNames, 'taxonomy') && currentParams.taxonomyId === 'new';

    return (
      <div styleName="fc-entries-wrapper">
        <h3>MERCHANDISING</h3>
        {this.taxonomiesList}
        <li>
          <NavigationItem
            to="taxonomies"
            icon="taxonomies"
            title="Manage Taxonomies"
            routes={routes}
            actualClaims={claims}
            expectedClaims={taxonomyClaims}
            currentParams={currentParams}
            forceActive={manageRoute}
          />
        </li>
      </div>
    );
  }
}

/*
 * Local redux store
 */
const fetch = createAsyncActions('fetchTaxonomies', searchTaxonomies);

const reducer = createReducer({
  [fetch.succeeded]: (state, response) => ({ ...state, taxonomies: get(response, 'result', []) }),
});

const mapState = state => ({
  taxonomies: state.taxonomies,
  fetchState: get(state.asyncActions, 'fetchTaxonomies', {}),
});

const mapGlobalState = state => ({
  createState: get(state.asyncActions, 'createTaxonomy', {}),
  updateState: get(state.asyncActions, 'updateTaxonomy', {}),
  archiveState: get(state.asyncActions, 'archiveTaxonomy', {}),
});

export default flow(
  connect(mapState, { fetch: fetch.perform }),
  makeLocalStore(addAsyncReducer(reducer), { taxonomies: [] }),
  connect(mapGlobalState),
)(MerchandisingEntry);
