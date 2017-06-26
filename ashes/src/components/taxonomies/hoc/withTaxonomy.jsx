/* @flow */

import { identity, get, flow, omit } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';
import { createReducer } from 'redux-act';
import { makeLocalStore, addAsyncReducer } from '@foxcomm/wings';

// actions
import { fetchTaxonomyInternal as fetch } from 'modules/taxonomies/details';

// helpers
import { getDisplayName } from 'lib/react-utils';

// components
import Spinner from 'components/core/spinner';

// styles
import s from './taxonomies.css';

const omitProps = ['taxonomy', 'fetch'];

type Props = {
  context: string,
  taxonomyId: string,
  taxonomy: Taxonomy,
  fetch: (id: number | string) => Promise<*>,
  fetchState: AsyncState,
};

type Options = {
  showLoader?: boolean,
  mapState?: Function,
  mapActions?: Function | {},
};

const defaultOptions: Options = {
  showLoader: true,
  mapState: identity,
  mapActions: {},
};

/**
 * Higher Order Component for mapping taxonomy to underlying component
 *
 * @param {Object}  options             HOC Options
 * @param {boolean} options.showLoader  If to show wait loader instead wrapped component on fetching.
 *                                      Instead handle loading in wrapped component
 * @param {boolean} options.mapState    Redux's mapStateToProps function to map additional state
 * @param {boolean} options.mapActions  Redux's mapDispatchToProps function to map additional actions
 *
 * @returns {Function}
 */
export default function withTaxonomy(options: Options = defaultOptions) {
  // TODO: proper type for component argument
  return function(WrappedComponent: any) {
    class Wrapper extends Component {
      props: Props;

      componentDidMount() {
        const { taxonomy, taxonomyId, context } = this.props;

        if (!taxonomy) {
          this.props.fetch(taxonomyId, context);
        }
      }

      render() {
        const { taxonomy, fetchState } = this.props;

        if (options.showLoader && (!taxonomy || (fetchState.inProgress && !fetchState.err))) {
          return <Spinner className={s.spinner} />;
        }

        const props = {
          [get(options, 'taxonomyField', 'taxonomy')]: this.props.taxonomy,
          [get(options, 'fetchStateField', 'fetchState')]: this.props.fetchState,
          [get(options, 'fetchField', 'fetchTaxonomy')]: this.props.fetch,
          ...omit(this.props, omitProps),
        };

        return <WrappedComponent {...props} />;
      }
    }

    Wrapper.displayName = `WithTaxonomy(${getDisplayName(WrappedComponent)})`;

    /*
     * Local redux store
     */
    const reducer = createReducer({
      [fetch.succeeded]: (state, response) => ({ ...state, taxonomy: response }),
    });

    const mapState = (state, props) => ({
      taxonomy: state.taxonomy,
      fetchState: get(state.asyncActions, 'fetchTaxonomy', {}),
    });

    return flow(
      connect(options.mapState, options.mapActions),
      connect(mapState, { fetch: fetch.perform }),
      makeLocalStore(addAsyncReducer(reducer), { taxonomy: null })
    )(Wrapper);
  };
}
