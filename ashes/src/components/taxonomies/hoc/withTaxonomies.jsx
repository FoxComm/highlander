/* @flow */

import { isEmpty, get, merge, omit } from 'lodash';
import React, { Component } from 'react';
import { connect } from 'react-redux';

// actions
import { fetch } from 'modules/taxonomies/flatList';

// helpers
import { getDisplayName } from 'lib/react-utils';

// components
import Spinner from 'components/core/spinner';

// styles
import s from './taxonomies.css';

const omitProps = ['taxonomy', 'taxonomies', 'fetch', 'createState', 'updateState', 'archiveState'];

type Props = {
  taxonomies: Array<TaxonomyResult>,
  fetch: () => Promise<*>,
  fetchState: AsyncState,
  archiveState: AsyncState,
  createState: AsyncState,
  updateState: AsyncState,
};

type Options = {
  showLoader?: boolean,
  taxonomiesField: string,
  fetchStateField: string,
};

const defaultOptions: Options = {
  showLoader: true,
  taxonomiesField: 'taxonomies',
  fetchStateField: 'fetchState',
};

/**
 * Higher Order Component for mapping taxonomies list to underlying component
 *
 * @param {Object}  options                   HOC Options
 * @param {boolean} options.showLoader        If to show wait loader instead wrapped component on fetching.
 *                                            Instead handle loading in wrapped component
 * @param {boolean} options.taxonomiesField   The name of prop field that taxonomies list would be mapped to
 * @param {boolean} options.fetchStateField   The name of prop field that fetchState would be mapped to
 *
 * @returns {Function}
 */
export default function withTaxonomies(options: Options) {
  options = merge({}, defaultOptions, options);

  // TODO: proper type for component argument
  return function(WrappedComponent: any) {
    class Wrapper extends Component {
      props: Props;

      componentDidMount() {
        if (isEmpty(this.props.taxonomies)) {
          this.props.fetch();
        }
      }

      componentWillReceiveProps(nextProps: Props) {
        const created = this.props.createState.inProgress && !nextProps.createState.inProgress;
        const updated = this.props.updateState.inProgress && !nextProps.updateState.inProgress;
        const archived = this.props.archiveState.inProgress && !nextProps.archiveState.inProgress;

        if (created || updated || archived) {
          this.props.fetch();
        }
      }

      render() {
        const { taxonomies, fetchState } = this.props;

        if (options.showLoader && (!taxonomies || (fetchState.inProgress && !fetchState.err))) {
          return <Spinner className={s.spinner} />;
        }

        const props = {
          [options.taxonomiesField]: this.props.taxonomies,
          [options.fetchStateField]: this.props.fetchState,
          ...omit(this.props, omitProps),
        };

        return <WrappedComponent {...props} />;
      }
    }

    Wrapper.displayName = `WithTaxonomies(${getDisplayName(WrappedComponent)})`;

    const mapState = state => ({
      taxonomies: state.taxonomies.flatList,
      fetchState: get(state.asyncActions, 'fetchTaxonomies', {}),
      createState: get(state.asyncActions, 'createTaxonomy', {}),
      updateState: get(state.asyncActions, 'updateTaxonomy', {}),
      archiveState: get(state.asyncActions, 'archiveTaxonomy', {}),
    });

    return connect(mapState, { fetch })(Wrapper);
  };
}
